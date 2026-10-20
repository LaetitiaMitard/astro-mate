package fr.astromate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
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

    MeasurementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        String legacyIndicatorKey = LEGACY_INDICATOR_KEYS.get(metricKey);
        if (legacyIndicatorKey != null) {
            updateLegacyIndicator(legacyIndicatorKey, canonicalValue, measuredAt);
        } else if (metricKey.equals("radiatorATemperature")
                || metricKey.equals("radiatorBTemperature")) {
            updateLegacyTemperatureProjection(measuredAt);
        }
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
}
