package fr.astromate.application.service;

import fr.astromate.application.port.MeasurementChanges;
import fr.astromate.application.port.MeasurementRepository;
import fr.astromate.application.port.Transactions;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.StationCatalog;
import fr.astromate.domain.model.TechnicalEvent;
import fr.astromate.domain.service.MeasurementTransitions;
import fr.astromate.domain.service.MetricStatusEvaluator;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

public class MeasurementService {
    private static final Set<String> RECORDABLE_METRICS = StationCatalog.METRIC_METADATA.keySet();
    private static final Set<String> AVAILABILITY_VALUES = Set.of("ONLINE", "OFFLINE");

    private final MeasurementRepository repository;
    private final MeasurementChanges changes;
    private final Transactions transactions;
    private final Clock clock;

    public MeasurementService(
            MeasurementRepository repository, MeasurementChanges changes,
            Transactions transactions, Clock clock
    ) {
        this.repository = repository;
        this.changes = changes;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void recordMeasurement(String metricKey, double canonicalValue, Instant measuredAt) {
        transactions.execute(() -> record(metricKey, canonicalValue, measuredAt));
    }

    private void record(String metricKey, double value, Instant measuredAt) {
        if (metricKey == null || !RECORDABLE_METRICS.contains(metricKey)) {
            throw new IllegalArgumentException("Métrique non enregistrable : " + metricKey);
        }
        if (measuredAt == null) {
            throw new IllegalArgumentException("L’horodatage de mesure est obligatoire.");
        }
        MeasurementSnapshot metric = repository.findMeasurement(metricKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Métrique non initialisée : " + metricKey));
        if ("OFFLINE".equals(repository.availability(metric.systemId()))) {
            return;
        }
        if (!MetricStatusEvaluator.isPhysicallyValid(metricKey, value)) {
            if (repository.startInvalidData(metricKey, measuredAt)) {
                event(metric, "WARNING", "Mesure invalide ignorée pour " + metric.name()
                        + " : valeur hors du domaine physique.", measuredAt, "DATA");
                changes.publish();
            }
            return;
        }
        if (measuredAt.isBefore(Instant.parse(metric.measuredAt()))) {
            throw new IllegalArgumentException(
                    "Une mesure ne peut pas remplacer une mesure plus récente pour "
                            + metricKey + ".");
        }
        if (!MetricStatusEvaluator.isStale(measuredAt, clock.instant())) {
            repository.clearStaleness(metricKey);
        }
        MetricStatus nextStatus = MetricStatusEvaluator.evaluate(metricKey, value);
        if (MeasurementTransitions.closesInvalidData(nextStatus)
                && repository.closeInvalidData(metricKey)) {
            event(metric, "INFO", "Données valides rétablies pour " + metric.name() + ".",
                    measuredAt, "DATA");
        }
        repository.saveMeasurement(metricKey, value, measuredAt);
        recordStatusTransition(metric, nextStatus, measuredAt);
        updateAlertEpisode(metric, nextStatus, measuredAt);
        repository.updateCompatibilityProjection(metricKey, value, measuredAt);
        changes.publish();
    }

    public void recordStaleMeasurements(Instant now) {
        transactions.execute(() -> {
            boolean changed = false;
            for (MeasurementSnapshot metric : repository.onlineMeasurements()) {
                if (!MetricStatusEvaluator.isPhysicallyValid(metric.metricKey(), metric.value())
                        || !MetricStatusEvaluator.ruleFor(metric.metricKey()).unit().equals(metric.unit())
                        || !MetricStatusEvaluator.isStale(Instant.parse(metric.measuredAt()), now)) {
                    continue;
                }
                if (repository.startStaleness(metric.metricKey(), now)) {
                    event(metric, "WARNING", "La dernière mesure valide de " + metric.name()
                            + " est périmée.", now, "DATA");
                    changed = true;
                }
            }
            if (changed) {
                changes.publish();
            }
        });
    }

    private void recordStatusTransition(
            MeasurementSnapshot metric, MetricStatus nextStatus, Instant occurredAt
    ) {
        MetricStatus previousStatus = MetricStatusEvaluator.evaluate(metric.metricKey(), metric.value());
        if (!MetricStatusEvaluator.ruleFor(metric.metricKey()).unit().equals(metric.unit())) {
            previousStatus = MetricStatus.UNKNOWN;
        }
        if (previousStatus != nextStatus) {
            event(metric, MeasurementTransitions.isAlert(nextStatus) ? nextStatus.name() : "INFO",
                    "État de " + metric.name() + " passé de "
                            + MeasurementTransitions.statusLabel(previousStatus) + " à "
                            + MeasurementTransitions.statusLabel(nextStatus) + ".",
                    occurredAt, "ALERT");
        }
    }

    private void updateAlertEpisode(
            MeasurementSnapshot metric, MetricStatus nextStatus, Instant occurredAt
    ) {
        var openEpisode = repository.openAlert(metric.metricKey()).orElse(null);
        if (openEpisode != null && openEpisode.level().equals(nextStatus.name())) {
            return;
        }
        if (openEpisode != null) {
            repository.resolveAlert(openEpisode.id(), occurredAt);
            event(metric, "INFO", "Alerte "
                    + MeasurementTransitions.alertLevelLabel(openEpisode.level())
                    + " résolue pour " + metric.name() + ".", occurredAt, "ALERT");
        }
        if (MeasurementTransitions.isAlert(nextStatus)) {
            String message = "Seuil " + MeasurementTransitions.alertLevelLabel(nextStatus.name())
                    + " dépassé pour " + metric.name() + ".";
            repository.createAlert(metric, nextStatus, message, occurredAt);
            event(metric, nextStatus.name(), message, occurredAt, "ALERT");
        }
    }

    public void setSystemAvailability(String systemId, String availability) {
        transactions.execute(() -> {
            if (systemId == null || availability == null
                    || StationCatalog.systemName(systemId) == null
                    || !AVAILABILITY_VALUES.contains(availability)) {
                throw new IllegalArgumentException("Disponibilité système invalide.");
            }
            if (repository.changeAvailability(systemId, availability)) {
                boolean offline = "OFFLINE".equals(availability);
                repository.appendEvent(new TechnicalEvent(
                        "simulation", systemId, offline ? "WARNING" : "INFO",
                        StationCatalog.systemName(systemId)
                                + (offline ? " simulé hors ligne." : " simulé de retour en ligne."),
                        clock.instant(), "SYSTEM"
                ));
                changes.publish();
            }
        });
    }

    private void event(
            MeasurementSnapshot metric, String level, String message,
            Instant occurredAt, String category
    ) {
        repository.appendEvent(new TechnicalEvent(
                metric.metricKey(), metric.systemId(), level, message, occurredAt, category));
    }
}
