package fr.astromate;

import fr.astromate.SystemDetailResponse.Component;
import fr.astromate.SystemDetailResponse.CriticalLimits;
import fr.astromate.SystemDetailResponse.Metric;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
class SystemDetailService {
    private static final List<SystemMetadata> SYSTEMS = List.of(
            new SystemMetadata(
                    "life-support",
                    "Support de vie",
                    "Atmosphère et qualité de l’air",
                    List.of("oxygen", "carbonDioxide"),
                    List.of()
            ),
            new SystemMetadata(
                    "thermal-control",
                    "Contrôle thermique",
                    "Température et circulation thermique",
                    List.of(
                            "radiatorATemperature",
                            "radiatorBTemperature",
                            "fluidFlow",
                            "pressure",
                            "thermalConsumption"
                    ),
                    List.of(
                            new ComponentMetadata(
                                    "radiator-a",
                                    "Radiateur A",
                                    List.of("radiatorATemperature")
                            ),
                            new ComponentMetadata(
                                    "radiator-b",
                                    "Radiateur B",
                                    List.of("radiatorBTemperature")
                            ),
                            new ComponentMetadata(
                                    "main-pump",
                                    "Pompe principale",
                                    List.of()
                            ),
                            new ComponentMetadata(
                                    "heat-exchanger",
                                    "Échangeur thermique",
                                    List.of()
                            )
                    )
            ),
            new SystemMetadata(
                    "power-supply",
                    "Alimentation électrique",
                    "Énergie disponible à bord",
                    List.of("availableEnergy"),
                    List.of()
            ),
            new SystemMetadata(
                    "communications",
                    "Communications",
                    "Système de communication de la station",
                    List.of(),
                    List.of()
            )
    );
    private static final Map<String, MetricMetadata> METRIC_METADATA = Map.ofEntries(
            Map.entry(
                    "oxygen",
                    new MetricMetadata("oxygen-level", "O₂", null)
            ),
            Map.entry(
                    "carbonDioxide",
                    new MetricMetadata("carbon-dioxide-level", "CO₂", null)
            ),
            Map.entry(
                    "radiatorATemperature",
                    new MetricMetadata(
                            "radiator-a-temperature",
                            "Température Radiateur A",
                            "radiator-a"
                    )
            ),
            Map.entry(
                    "radiatorBTemperature",
                    new MetricMetadata(
                            "radiator-b-temperature",
                            "Température Radiateur B",
                            "radiator-b"
                    )
            ),
            Map.entry(
                    "fluidFlow",
                    new MetricMetadata("fluid-flow", "Débit de fluide", null)
            ),
            Map.entry(
                    "pressure",
                    new MetricMetadata("pressure", "Pression", null)
            ),
            Map.entry(
                    "thermalConsumption",
                    new MetricMetadata(
                            "thermal-consumption",
                            "Consommation thermique",
                            null
                    )
            ),
            Map.entry(
                    "availableEnergy",
                    new MetricMetadata(
                            "available-energy",
                            "Énergie disponible",
                            null
                    )
            )
    );

    private final JdbcTemplate jdbcTemplate;
    private final DashboardService dashboardService;
    private final Instant simulatedStateStartedAt = Instant.now();

    SystemDetailService(JdbcTemplate jdbcTemplate, DashboardService dashboardService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dashboardService = dashboardService;
    }

    SystemDetailResponse getSystemDetail(String systemId) {
        SystemMetadata system = SYSTEMS.stream()
                .filter(candidate -> candidate.id().equals(systemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Système inconnu."
                ));
        Map<String, CurrentMeasurement> measurements = readMeasurements();
        DashboardResponse dashboard = dashboardService.getDashboard();
        Map<String, String> statuses = new LinkedHashMap<>();
        dashboard.systemStatuses()
                .forEach(state -> statuses.put(state.systemId(), state.status()));
        Map<String, String> availability = readAvailability();
        Instant now = Instant.now();

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
        List<DashboardResponse.Indicator> indicators = dashboard.indicators().stream()
                .filter(indicator -> indicator.systemId().equals(systemId))
                .toList();

        return new SystemDetailResponse(
                system.id(),
                system.name(),
                system.description(),
                statuses.get(systemId),
                availability.get(systemId),
                latestMeasurementAt(system.id(), measurements),
                indicators,
                metrics,
                components,
                List.of()
        );
    }

    private Map<String, CurrentMeasurement> readMeasurements() {
        List<CurrentMeasurement> rows = jdbcTemplate.query(
                """
                SELECT metric_key, name, value, unit, measured_at
                FROM metric_measurement
                """,
                (resultSet, rowNumber) -> new CurrentMeasurement(
                        resultSet.getString("metric_key"),
                        resultSet.getString("name"),
                        resultSet.getDouble("value"),
                        resultSet.getString("unit"),
                        resultSet.getString("measured_at")
                )
        );
        Map<String, CurrentMeasurement> measurements = new LinkedHashMap<>();
        rows.forEach(measurement -> measurements.put(
                measurement.metricKey(),
                measurement
        ));
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

    private static Metric metric(
            String metricKey,
            CurrentMeasurement measurement,
            Instant now
    ) {
        MetricStatusEvaluator.Rule rule = MetricStatusEvaluator.ruleFor(metricKey);
        MetricMetadata metadata = METRIC_METADATA.get(metricKey);
        boolean valid = isValid(measurement, rule);
        return new Metric(
                metadata.id(),
                measurement == null ? metadata.name() : measurement.name(),
                rule.unit(),
                valid ? measurement.value() : null,
                status(metricKey, measurement, now).name(),
                new DashboardResponse.Range(
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
            Map<String, CurrentMeasurement> measurements,
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
                .map(key -> status(key, measurements.get(key), now))
                .toList();
        String status = MetricStatusEvaluator.mostSevere(metricStatuses).name();
        CurrentMeasurement measurement = metadata.metricKeys().stream()
                .map(measurements::get)
                .filter(value -> isValid(
                        value,
                        MetricStatusEvaluator.ruleFor(
                                metadata.metricKeys().get(0)
                        )
                ))
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

    private static MetricStatus status(
            String metricKey,
            CurrentMeasurement measurement,
            Instant now
    ) {
        if (measurement == null) {
            return MetricStatus.UNKNOWN;
        }
        MetricStatus evaluated = MetricStatusEvaluator.evaluate(
                metricKey,
                measurement.value()
        );
        if (evaluated == MetricStatus.UNKNOWN
                || !MetricStatusEvaluator.ruleFor(metricKey)
                        .unit().equals(measurement.unit())) {
            return MetricStatus.UNKNOWN;
        }
        return Instant.parse(measurement.measuredAt())
                        .isBefore(now.minusSeconds(30))
                ? MetricStatus.UNKNOWN
                : evaluated;
    }

    private static boolean isValid(
            CurrentMeasurement measurement,
            MetricStatusEvaluator.Rule rule
    ) {
        return measurement != null
                && MetricStatusEvaluator.isPhysicallyValid(
                        measurement.metricKey(),
                        measurement.value()
                )
                && rule.unit().equals(measurement.unit());
    }

    private static String latestMeasurementAt(
            String systemId,
            Map<String, CurrentMeasurement> measurements
    ) {
        List<String> metricKeys = SYSTEMS.stream()
                .filter(system -> system.id().equals(systemId))
                .findFirst()
                .orElseThrow()
                .metricKeys();
        return measurements.values().stream()
                .filter(measurement -> metricKeys.contains(measurement.metricKey()))
                .filter(measurement -> isValid(
                        measurement,
                        MetricStatusEvaluator.ruleFor(measurement.metricKey())
                ))
                .max(Comparator.comparing(
                        measurement -> Instant.parse(measurement.measuredAt())
                ))
                .map(CurrentMeasurement::measuredAt)
                .orElse(null);
    }

    private record SystemMetadata(
            String id,
            String name,
            String description,
            List<String> metricKeys,
            List<ComponentMetadata> components
    ) {
    }

    private record ComponentMetadata(
            String id,
            String name,
            List<String> metricKeys
    ) {
    }

    private record CurrentMeasurement(
            String metricKey,
            String name,
            double value,
            String unit,
            String measuredAt
    ) {
    }

    private record MetricMetadata(String id, String name, String componentId) {
    }

    private record SystemAvailability(String systemId, String availability) {
    }
}
