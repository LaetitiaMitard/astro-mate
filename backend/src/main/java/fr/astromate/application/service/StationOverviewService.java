package fr.astromate.application.service;

import fr.astromate.application.model.DashboardView.Indicator;
import fr.astromate.application.model.DashboardView.Range;
import fr.astromate.application.model.StationOverview;
import fr.astromate.application.port.StationRepository;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.service.MetricStatusEvaluator;
import fr.astromate.domain.service.StationStatusCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static fr.astromate.domain.service.StationStatusCalculator.isPhysicallyValid;
import static fr.astromate.domain.service.StationStatusCalculator.statusOf;
import static fr.astromate.domain.service.StationStatusCalculator.systemStatuses;

public class StationOverviewService {
    private final StationRepository repository;
    private final Clock clock;

    public StationOverviewService(StationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public StationOverview getOverview() {
        Map<String, MeasurementSnapshot> measurements = repository.readMeasurements();
        Map<String, String> availability = repository.readAvailability();
        Instant now = clock.instant();
        return new StationOverview(
                measurements, availability, systemStatuses(measurements, availability, now),
                List.of(indicator("oxygen", measurements.get("oxygen"), now),
                        indicator("carbonDioxide", measurements.get("carbonDioxide"), now),
                        temperatureIndicator(measurements, now),
                        indicator("availableEnergy", measurements.get("availableEnergy"), now)));
    }

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

    private static Indicator indicator(
            String metricKey,
            MeasurementSnapshot measurement,
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
            Map<String, MeasurementSnapshot> measurements,
            Instant now
    ) {
        MeasurementSnapshot radiatorA = measurements.get("radiatorATemperature");
        MeasurementSnapshot radiatorB = measurements.get("radiatorBTemperature");
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

    private static Double validValue(MeasurementSnapshot measurement) {
        return isPhysicallyValid(measurement) ? measurement.value() : null;
    }

    private static String latestMeasurementAt(
            MeasurementSnapshot first,
            MeasurementSnapshot second
    ) {
        return java.util.stream.Stream.of(first, second)
                .filter(StationStatusCalculator::isPhysicallyValid)
                .max(Comparator.comparing(
                        measurement -> Instant.parse(measurement.measuredAt())
                ))
                .map(MeasurementSnapshot::measuredAt)
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

    private record IndicatorMetadata(
            String systemId,
            List<String> sourceMetricIds,
            String name
    ) {
    }
}
