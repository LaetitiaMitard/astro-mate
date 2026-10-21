package fr.astromate;

import fr.astromate.DashboardResponse.Indicator;
import fr.astromate.DashboardResponse.Range;
import fr.astromate.DashboardResponse.SystemState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class DashboardService {
    private static final List<String> SYSTEM_IDS = List.of(
            "life-support",
            "thermal-control",
            "power-supply",
            "communications"
    );
    private static final Map<String, List<String>> REQUIRED_METRICS = Map.of(
            "life-support", List.of("oxygen", "carbonDioxide"),
            "thermal-control", List.of(
                    "radiatorATemperature",
                    "radiatorBTemperature",
                    "fluidFlow",
                    "pressure",
                    "thermalConsumption"
            ),
            "power-supply", List.of("availableEnergy"),
            "communications", List.of()
    );
    private static final Map<String, IndicatorMetadata> INDICATOR_METADATA = Map.of(
            "oxygen", new IndicatorMetadata(
                    "life-support",
                    List.of("oxygen-level"),
                    "O₂"
            ),
            "carbonDioxide", new IndicatorMetadata(
                    "life-support",
                    List.of("carbon-dioxide-level"),
                    "CO₂"
            ),
            "temperature", new IndicatorMetadata(
                    "thermal-control",
                    List.of("radiator-a-temperature", "radiator-b-temperature"),
                    "Température"
            ),
            "availableEnergy", new IndicatorMetadata(
                    "power-supply",
                    List.of("available-energy"),
                    "Énergie disponible"
            )
    );

    private final JdbcTemplate jdbcTemplate;

    DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    DashboardResponse getDashboard() {
        Map<String, Measurement> measurements = readMeasurements();
        Map<String, String> availability = readAvailability();
        Instant now = Instant.now();

        Indicator oxygen = indicator(
                "oxygen",
                measurements.get("oxygen"),
                now
        );
        Indicator carbonDioxide = indicator(
                "carbonDioxide",
                measurements.get("carbonDioxide"),
                now
        );
        Indicator temperature = temperatureIndicator(measurements, now);
        Indicator availableEnergy = indicator(
                "availableEnergy",
                measurements.get("availableEnergy"),
                now
        );
        List<Indicator> indicators = List.of(
                oxygen,
                carbonDioxide,
                temperature,
                availableEnergy
        );

        Map<String, MetricStatus> systemStatuses =
                systemStatuses(measurements, availability, now);
        MetricStatus stationStatus = MetricStatusEvaluator.mostSevere(
                new ArrayList<>(systemStatuses.values())
        );
        List<String> offlineSystems = SYSTEM_IDS.stream()
                .filter(systemId -> "OFFLINE".equals(availability.get(systemId)))
                .toList();
        List<SystemState> systemStateSummaries = SYSTEM_IDS.stream()
                .map(systemId -> new SystemState(
                        systemId,
                        systemStatuses.get(systemId).name()
                ))
                .toList();
        String lastMeasurementAt = measurements.values().stream()
                .filter(measurement -> isPhysicallyValid(measurement))
                .max(Comparator.comparing(
                        measurement -> Instant.parse(measurement.measuredAt())
                ))
                .map(Measurement::measuredAt)
                .orElse(null);

        return new DashboardResponse(
                stationStatus.name(),
                offlineSystems,
                systemStateSummaries,
                "ACTIVE",
                lastMeasurementAt,
                indicators,
                readRecentOpenAlerts()
        );
    }

    private List<DashboardResponse.Alert> readRecentOpenAlerts() {
        return jdbcTemplate.query(
                """
                SELECT id, level, system_id, metric_id, message, status,
                       detected_at, resolved_at
                FROM alert_episode
                WHERE status = 'OPEN'
                ORDER BY CASE level WHEN 'CRITICAL' THEN 0 ELSE 1 END,
                         detected_at DESC
                LIMIT 4
                """,
                (resultSet, rowNumber) -> new DashboardResponse.Alert(
                        resultSet.getString("id"),
                        resultSet.getString("level"),
                        resultSet.getString("system_id"),
                        systemName(resultSet.getString("system_id")),
                        resultSet.getString("metric_id"),
                        resultSet.getString("message"),
                        resultSet.getString("status"),
                        resultSet.getString("detected_at"),
                        resultSet.getString("resolved_at")
                )
        );
    }

    private static String systemName(String systemId) {
        return switch (systemId) {
            case "life-support" -> "Support de vie";
            case "thermal-control" -> "Contrôle thermique";
            case "power-supply" -> "Alimentation électrique";
            case "communications" -> "Communications";
            default -> throw new IllegalStateException(
                    "Système inattendu dans une alerte : " + systemId
            );
        };
    }

    private Map<String, Measurement> readMeasurements() {
        List<Measurement> rows = jdbcTemplate.query(
                """
                SELECT metric_key, system_id, metric_id, name, value, unit, measured_at
                FROM metric_measurement
                """,
                (resultSet, rowNumber) -> new Measurement(
                        resultSet.getString("metric_key"),
                        resultSet.getString("system_id"),
                        resultSet.getString("metric_id"),
                        resultSet.getString("name"),
                        resultSet.getDouble("value"),
                        resultSet.getString("unit"),
                        resultSet.getString("measured_at")
                )
        );
        Map<String, Measurement> measurements = new LinkedHashMap<>();
        rows.forEach(measurement -> measurements.put(measurement.metricKey(), measurement));
        return measurements;
    }

    private Map<String, String> readAvailability() {
        List<SystemAvailability> rows = jdbcTemplate.query(
                "SELECT system_id, availability FROM system_availability",
                (resultSet, rowNumber) -> new SystemAvailability(
                        resultSet.getString("system_id"),
                        resultSet.getString("availability")
                )
        );
        Map<String, String> availability = new LinkedHashMap<>();
        rows.forEach(row -> availability.put(row.systemId(), row.availability()));
        return availability;
    }

    private static Indicator indicator(
            String metricKey,
            Measurement measurement,
            Instant now
    ) {
        IndicatorMetadata metadata = INDICATOR_METADATA.get(metricKey);
        MetricStatusEvaluator.Rule rule = MetricStatusEvaluator.ruleFor(metricKey);
        MetricStatus status = statusOf(measurement, now);
        return new Indicator(
                metricKey,
                measurement == null ? metadata.systemId() : measurement.systemId(),
                measurement == null
                        ? metadata.sourceMetricIds()
                        : List.of(measurement.metricId()),
                metadata.name(),
                validValue(measurement),
                rule.unit(),
                status.name(),
                range(rule),
                isPhysicallyValid(measurement) ? measurement.measuredAt() : null
        );
    }

    private static Indicator temperatureIndicator(
            Map<String, Measurement> measurements,
            Instant now
    ) {
        Measurement radiatorA = measurements.get("radiatorATemperature");
        Measurement radiatorB = measurements.get("radiatorBTemperature");
        List<MetricStatus> sourceStatuses = List.of(
                statusOf(radiatorA, now),
                statusOf(radiatorB, now)
        );
        Double average = null;
        if (isPhysicallyValid(radiatorA) && isPhysicallyValid(radiatorB)) {
            average = BigDecimal.valueOf(
                            radiatorA.value() / 2 + radiatorB.value() / 2)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        String measuredAt = latestMeasurementAt(radiatorA, radiatorB);
        MetricStatusEvaluator.Rule rule =
                MetricStatusEvaluator.ruleFor("temperature");
        IndicatorMetadata metadata = INDICATOR_METADATA.get("temperature");
        return new Indicator(
                "temperature",
                metadata.systemId(),
                metadata.sourceMetricIds(),
                metadata.name(),
                average,
                rule.unit(),
                MetricStatusEvaluator.mostSevere(sourceStatuses).name(),
                range(rule),
                measuredAt
        );
    }

    private static Map<String, MetricStatus> systemStatuses(
            Map<String, Measurement> measurements,
            Map<String, String> availability,
            Instant now
    ) {
        Map<String, MetricStatus> statuses = new LinkedHashMap<>();
        for (String systemId : SYSTEM_IDS) {
            List<MetricStatus> metricStatuses = REQUIRED_METRICS.get(systemId).stream()
                    .map(metricKey -> statusOf(measurements.get(metricKey), now))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            String systemAvailability = availability.get(systemId);
            if ("OFFLINE".equals(systemAvailability)) {
                metricStatuses.add(MetricStatus.OFFLINE);
            } else if (!"ONLINE".equals(systemAvailability)) {
                metricStatuses.add(MetricStatus.UNKNOWN);
            }
            statuses.put(
                    systemId,
                    MetricStatusEvaluator.mostSevere(metricStatuses)
            );
        }
        return statuses;
    }

    private static MetricStatus statusOf(Measurement measurement, Instant now) {
        if (measurement == null) {
            return MetricStatus.UNKNOWN;
        }
        MetricStatus status = MetricStatusEvaluator.evaluate(
                measurement.metricKey(),
                measurement.value()
        );
        if (status == MetricStatus.UNKNOWN) {
            return status;
        }
        if (!measurement.unit().equals(
                MetricStatusEvaluator.ruleFor(measurement.metricKey()).unit())) {
            return MetricStatus.UNKNOWN;
        }
        Instant measuredAt = Instant.parse(measurement.measuredAt());
        return measuredAt.isBefore(now.minusSeconds(30))
                ? MetricStatus.UNKNOWN
                : status;
    }

    private static Double validValue(Measurement measurement) {
        return isPhysicallyValid(measurement) ? measurement.value() : null;
    }

    private static boolean isPhysicallyValid(Measurement measurement) {
        return measurement != null
                && MetricStatusEvaluator.isPhysicallyValid(
                        measurement.metricKey(),
                        measurement.value()
                )
                && measurement.unit().equals(
                        MetricStatusEvaluator.ruleFor(measurement.metricKey()).unit());
    }

    private static String latestMeasurementAt(
            Measurement first,
            Measurement second
    ) {
        return java.util.stream.Stream.of(first, second)
                .filter(DashboardService::isPhysicallyValid)
                .max(Comparator.comparing(
                        measurement -> Instant.parse(measurement.measuredAt())
                ))
                .map(Measurement::measuredAt)
                .orElse(null);
    }

    private static Range range(MetricStatusEvaluator.Rule rule) {
        return new Range(
                rule.nominalMin(),
                rule.nominalMax(),
                rule.minInclusive(),
                rule.maxInclusive()
        );
    }

    private record Measurement(
            String metricKey,
            String systemId,
            String metricId,
            String name,
            double value,
            String unit,
            String measuredAt
    ) {
    }

    private record SystemAvailability(String systemId, String availability) {
    }

    private record IndicatorMetadata(
            String systemId,
            List<String> sourceMetricIds,
            String name
    ) {
    }
}
