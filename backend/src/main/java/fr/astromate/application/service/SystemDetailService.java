package fr.astromate.application.service;

import fr.astromate.application.model.ApplicationException;
import fr.astromate.application.model.DashboardView;
import fr.astromate.application.model.StationOverview;
import fr.astromate.application.model.SystemDetailView;
import fr.astromate.application.model.SystemDetailView.Component;
import fr.astromate.application.model.SystemDetailView.CriticalLimits;
import fr.astromate.application.model.SystemDetailView.Metric;
import fr.astromate.application.model.SystemSummaryView;
import fr.astromate.application.port.SystemDetailQueries;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.StationCatalog.ComponentMetadata;
import fr.astromate.domain.model.StationCatalog.MetricMetadata;
import fr.astromate.domain.model.StationCatalog.SystemMetadata;
import fr.astromate.domain.service.MetricStatusEvaluator;
import fr.astromate.domain.service.StationStatusCalculator;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static fr.astromate.domain.model.StationCatalog.METRIC_METADATA;
import static fr.astromate.domain.model.StationCatalog.SYSTEMS;

public class SystemDetailService {
    private final StationOverviewService overviewService;
    private final SystemDetailQueries queries;
    private final Clock clock;

    public SystemDetailService(
            StationOverviewService overviewService, SystemDetailQueries queries, Clock clock
    ) {
        this.overviewService = overviewService;
        this.queries = queries;
        this.clock = clock;
    }

    public List<SystemSummaryView> getSystems() {
        StationOverview overview = overviewService.getOverview();
        var statuses = overview.statuses();
        var availability = overview.availability();
        var measurements = overview.measurements();

        return SYSTEMS.stream()
                .map(system -> new SystemSummaryView(
                        system.id(),
                        system.name(),
                        system.description(),
                        statuses.get(system.id()).name(),
                        availability.get(system.id()),
                        latestMeasurementAt(system.id(), measurements)
                ))
                .toList();
    }

    public SystemDetailView getSystemDetail(String systemId) {
        SystemMetadata system = SYSTEMS.stream()
                .filter(candidate -> candidate.id().equals(systemId))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        ApplicationException.Kind.NOT_FOUND,
                        "Système inconnu."
                ));
        StationOverview overview = overviewService.getOverview();
        var measurements = overview.measurements();
        var statuses = overview.statuses();
        var availability = overview.availability();
        Instant now = clock.instant();
        Instant simulatedStateStartedAt = queries.simulatedStateStartedAt();

        List<Metric> metrics = system.metricKeys().stream()
                .map(key -> metric(key, measurements.get(key), now))
                .toList();
        List<Component> components = system.components().stream()
                .map(component -> component(
                        component,
                        measurements,
                        now,
                        simulatedStateStartedAt
                ))
                .toList();
        List<DashboardView.Indicator> indicators = overview.indicators().stream()
                .filter(indicator -> indicator.systemId().equals(systemId))
                .toList();

        return new SystemDetailView(
                system.id(),
                system.name(),
                system.description(),
                statuses.get(systemId).name(),
                availability.get(systemId),
                latestMeasurementAt(system.id(), measurements),
                indicators,
                metrics,
                components,
                queries.recentEvents(system.id())
        );
    }

    private static Metric metric(
            String metricKey,
            MeasurementSnapshot measurement,
            Instant now
    ) {
        MetricStatusEvaluator.Rule rule = MetricStatusEvaluator.ruleFor(metricKey);
        MetricMetadata metadata = METRIC_METADATA.get(metricKey);
        boolean valid = StationStatusCalculator.isPhysicallyValid(measurement);
        return new Metric(
                metadata.id(),
                measurement == null ? metadata.name() : measurement.name(),
                rule.unit(),
                valid ? measurement.value() : null,
                StationStatusCalculator.statusOf(measurement, now).name(),
                new DashboardView.Range(
                        rule.nominalMin(),
                        rule.nominalMax(),
                        rule.minInclusive(),
                        rule.maxInclusive()
                ),
                new CriticalLimits(rule.criticalMin(), rule.criticalMax()),
                valid ? measurement.measuredAt() : null,
                metadata.componentId()
        );
    }

    private static Component component(
            ComponentMetadata metadata,
            Map<String, MeasurementSnapshot> measurements,
            Instant now,
            Instant simulatedStateStartedAt
    ) {
        if (metadata.metricKeys().isEmpty()) {
            return new Component(
                    metadata.id(),
                    metadata.name(),
                    "NOMINAL",
                    null,
                    null,
                    simulatedStateStartedAt.toString(),
                    true
            );
        }

        List<MetricStatus> metricStatuses = metadata.metricKeys().stream()
                .map(key -> StationStatusCalculator.statusOf(measurements.get(key), now))
                .toList();
        String status = MetricStatusEvaluator.mostSevere(metricStatuses).name();
        MeasurementSnapshot measurement = metadata.metricKeys().stream()
                .map(measurements::get)
                .filter(StationStatusCalculator::isPhysicallyValid)
                .max(Comparator.comparing(value -> Instant.parse(value.measuredAt())))
                .orElse(null);
        return new Component(
                metadata.id(),
                metadata.name(),
                status,
                measurement == null ? null : measurement.value(),
                measurement == null ? null : measurement.unit(),
                measurement == null ? null : measurement.measuredAt(),
                false
        );
    }

    private static String latestMeasurementAt(
            String systemId,
            Map<String, MeasurementSnapshot> measurements
    ) {
        List<String> metricKeys = SYSTEMS.stream()
                .filter(system -> system.id().equals(systemId))
                .findFirst()
                .orElseThrow()
                .metricKeys();
        return measurements.values().stream()
                .filter(measurement -> metricKeys.contains(measurement.metricKey()))
                .filter(StationStatusCalculator::isPhysicallyValid)
                .max(Comparator.comparing(
                        measurement -> Instant.parse(measurement.measuredAt())
                ))
                .map(MeasurementSnapshot::measuredAt)
                .orElse(null);
    }

}
