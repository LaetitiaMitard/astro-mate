package fr.astromate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MeasurementService {
    private static final Set<String> RECORDABLE_METRICS = Set.of(
            "oxygen",
            "carbonDioxide",
            "radiatorATemperature",
            "radiatorBTemperature",
            "availableEnergy",
            "fluidFlow",
            "pressure",
            "thermalConsumption"
    );
    private static final Set<String> SYSTEM_IDS = Set.of(
            "life-support",
            "thermal-control",
            "power-supply",
            "communications"
    );
    private static final Set<String> AVAILABILITY_VALUES = Set.of("ONLINE", "OFFLINE");
    private static final Map<String, String> LEGACY_INDICATOR_KEYS = Map.of(
            "oxygen", "oxygen",
            "carbonDioxide", "carbonDioxide",
            "availableEnergy", "availableEnergy"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    MeasurementService(
            JdbcTemplate jdbcTemplate,
            ApplicationEventPublisher eventPublisher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    void recordMeasurement(String metricKey, double canonicalValue, Instant measuredAt) {
        if (metricKey == null || !RECORDABLE_METRICS.contains(metricKey)) {
            throw new IllegalArgumentException("Métrique non enregistrable : " + metricKey);
        }
        if (!MetricStatusEvaluator.isPhysicallyValid(metricKey, canonicalValue)) {
            throw new IllegalArgumentException(
                    "La mesure est hors du domaine physique de " + metricKey
            );
        }
        if (measuredAt == null) {
            throw new IllegalArgumentException("L’horodatage de mesure est obligatoire.");
        }

        List<MetricSnapshot> previousMeasurements = jdbcTemplate.query(
                """
                SELECT system_id, metric_id, name, value, unit, measured_at
                FROM metric_measurement
                WHERE metric_key = ?
                """,
                (resultSet, rowNumber) -> new MetricSnapshot(
                        resultSet.getString("system_id"),
                        resultSet.getString("metric_id"),
                        resultSet.getString("name"),
                        resultSet.getDouble("value"),
                        resultSet.getString("unit"),
                        resultSet.getString("measured_at")
                ),
                metricKey
        );
        if (previousMeasurements.isEmpty()) {
            throw new IllegalArgumentException("Métrique non initialisée : " + metricKey);
        }
        MetricSnapshot metric = previousMeasurements.get(0);
        if (measuredAt.isBefore(Instant.parse(metric.measuredAt()))) {
            throw new IllegalArgumentException(
                    "Une mesure ne peut pas remplacer une mesure plus récente pour "
                            + metricKey + "."
            );
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE metric_measurement
                SET value = ?, unit = ?, measured_at = ?
                WHERE metric_key = ?
                """,
                canonicalValue,
                MetricStatusEvaluator.ruleFor(metricKey).unit(),
                measuredAt.toString(),
                metricKey
        );
        if (updated != 1) {
            throw new IllegalArgumentException("Métrique non initialisée : " + metricKey);
        }
        jdbcTemplate.update(
                """
                INSERT INTO measurement_history (metric_key, value, unit, measured_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(metric_key, measured_at) DO UPDATE
                SET value = excluded.value, unit = excluded.unit
                """,
                metricKey,
                canonicalValue,
                MetricStatusEvaluator.ruleFor(metricKey).unit(),
                measuredAt.toString()
        );
        jdbcTemplate.update(
                """
                DELETE FROM measurement_history
                WHERE julianday(measured_at) < julianday(?)
                """,
                Instant.now().minus(Duration.ofHours(6)).toString()
        );

        MetricStatus nextStatus = MetricStatusEvaluator.evaluate(
                metricKey,
                canonicalValue
        );
        recordStatusTransition(metricKey, metric, nextStatus, measuredAt);
        updateAlertEpisode(
                metricKey,
                metric,
                nextStatus,
                measuredAt
        );

        String legacyIndicatorKey = LEGACY_INDICATOR_KEYS.get(metricKey);
        if (legacyIndicatorKey != null) {
            updateLegacyIndicator(legacyIndicatorKey, canonicalValue, measuredAt);
        } else if (metricKey.equals("radiatorATemperature")
                || metricKey.equals("radiatorBTemperature")) {
            updateLegacyTemperatureProjection(measuredAt);
        }
        eventPublisher.publishEvent(new MeasurementChangeEvent());
    }

    private void recordStatusTransition(
            String metricKey,
            MetricSnapshot metric,
            MetricStatus nextStatus,
            Instant occurredAt
    ) {
        MetricStatus previousStatus = MetricStatusEvaluator.evaluate(
                metricKey,
                metric.value()
        );
        if (!MetricStatusEvaluator.ruleFor(metricKey).unit().equals(metric.unit())) {
            previousStatus = MetricStatus.UNKNOWN;
        }
        if (previousStatus == nextStatus) {
            return;
        }
        String level = nextStatus == MetricStatus.WARNING
                        || nextStatus == MetricStatus.CRITICAL
                ? nextStatus.name()
                : "INFO";
        String message = "État de " + metric.name() + " passé de "
                + statusLabel(previousStatus) + " à " + statusLabel(nextStatus) + ".";
        insertTechnicalEvent(
                metricKey,
                metric.systemId(),
                level,
                message,
                occurredAt
        );
    }

    private static String statusLabel(MetricStatus status) {
        return switch (status) {
            case NOMINAL -> "nominal";
            case WARNING -> "avertissement";
            case CRITICAL -> "critique";
            case UNKNOWN -> "inconnu";
            case OFFLINE -> "hors ligne";
        };
    }

    private void updateAlertEpisode(
            String metricKey,
            MetricSnapshot metric,
            MetricStatus nextStatus,
            Instant occurredAt
    ) {
        List<AlertEpisode> openEpisodes = jdbcTemplate.query(
                """
                SELECT id, level
                FROM alert_episode
                WHERE metric_key = ? AND status = 'OPEN'
                """,
                (resultSet, rowNumber) -> new AlertEpisode(
                        resultSet.getString("id"),
                        resultSet.getString("level")
                ),
                metricKey
        );
        AlertEpisode openEpisode = openEpisodes.isEmpty() ? null : openEpisodes.get(0);
        if (openEpisode != null && openEpisode.level().equals(nextStatus.name())) {
            return;
        }

        if (openEpisode != null) {
            resolveAlertEpisode(openEpisode, metricKey, metric, occurredAt);
        }
        if (nextStatus == MetricStatus.WARNING || nextStatus == MetricStatus.CRITICAL) {
            createAlertEpisode(metricKey, metric, nextStatus, occurredAt);
        }
    }

    private void resolveAlertEpisode(
            AlertEpisode episode,
            String metricKey,
            MetricSnapshot metric,
            Instant resolvedAt
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE alert_episode
                SET status = 'RESOLVED', resolved_at = ?
                WHERE id = ? AND status = 'OPEN'
                """,
                resolvedAt.toString(),
                episode.id()
        );
        if (updated != 1) {
            throw new IllegalStateException("L’alerte ouverte a changé pendant sa résolution.");
        }
        insertTechnicalEvent(
                metricKey,
                metric.systemId(),
                "INFO",
                "Alerte " + alertLevelLabel(episode.level())
                        + " résolue pour " + metric.name() + ".",
                resolvedAt
        );
    }

    private void createAlertEpisode(
            String metricKey,
            MetricSnapshot metric,
            MetricStatus level,
            Instant detectedAt
    ) {
        String message = "Seuil " + alertLevelLabel(level.name())
                + " dépassé pour " + metric.name() + ".";
        jdbcTemplate.update(
                """
                INSERT INTO alert_episode
                    (id, system_id, metric_id, metric_key, level, message, status,
                     detected_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, NULL)
                """,
                UUID.randomUUID().toString(),
                metric.systemId(),
                metric.metricId(),
                metricKey,
                level.name(),
                message,
                detectedAt.toString()
        );
        insertTechnicalEvent(
                metricKey,
                metric.systemId(),
                level.name(),
                message,
                detectedAt
        );
    }

    private static String alertLevelLabel(String level) {
        return switch (level) {
            case "WARNING" -> "d’avertissement";
            case "CRITICAL" -> "critique";
            default -> throw new IllegalArgumentException(
                    "Niveau d’alerte non pris en charge : " + level
            );
        };
    }

    private void insertTechnicalEvent(
            String metricKey,
            String systemId,
            String level,
            String message,
            Instant occurredAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO station_event
                    (id, system_id, level, category, source, message, occurred_at)
                VALUES (?, ?, ?, 'ALERT', ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                systemId,
                level,
                metricKey,
                message,
                occurredAt.toString()
        );
    }

    @Transactional
    void setSystemAvailability(String systemId, String availability) {
        if (systemId == null
                || availability == null
                || !SYSTEM_IDS.contains(systemId)
                || !AVAILABILITY_VALUES.contains(availability)) {
            throw new IllegalArgumentException("Disponibilité système invalide.");
        }
        int updated = jdbcTemplate.update(
                "UPDATE system_availability SET availability = ? WHERE system_id = ?",
                availability,
                systemId
        );
        if (updated != 1) {
            throw new IllegalArgumentException("Système non initialisé : " + systemId);
        }
        eventPublisher.publishEvent(new MeasurementChangeEvent());
    }

    private void updateLegacyIndicator(
            String indicatorKey,
            double value,
            Instant measuredAt
    ) {
        int updated = jdbcTemplate.update(
                """
                UPDATE dashboard_indicator
                SET value = ?, unit = ?, measured_at = ?
                WHERE indicator_key = ?
                """,
                value,
                MetricStatusEvaluator.ruleFor(indicatorKey).unit(),
                measuredAt.toString(),
                indicatorKey
        );
        if (updated != 1) {
            throw new IllegalStateException(
                    "Projection de compatibilité absente : " + indicatorKey
            );
        }
    }

    private void updateLegacyTemperatureProjection(Instant measuredAt) {
        double radiatorA = jdbcTemplate.queryForObject(
                "SELECT value FROM metric_measurement WHERE metric_key = ?",
                Double.class,
                "radiatorATemperature"
        );
        double radiatorB = jdbcTemplate.queryForObject(
                "SELECT value FROM metric_measurement WHERE metric_key = ?",
                Double.class,
                "radiatorBTemperature"
        );
        double average = BigDecimal.valueOf(radiatorA / 2 + radiatorB / 2)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        int updated = jdbcTemplate.update(
                """
                UPDATE dashboard_indicator
                SET value = ?, unit = ?, measured_at = ?
                WHERE indicator_key = 'temperature'
                """,
                average,
                MetricStatusEvaluator.ruleFor("temperature").unit(),
                measuredAt.toString()
        );
        if (updated != 1) {
            throw new IllegalStateException(
                    "Projection de compatibilité de température absente."
            );
        }
    }

    private record MetricSnapshot(
            String systemId,
            String metricId,
            String name,
            double value,
            String unit,
            String measuredAt
    ) {
    }

    private record AlertEpisode(String id, String level) {
    }
}
