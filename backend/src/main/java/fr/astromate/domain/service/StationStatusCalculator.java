package fr.astromate.domain.service;

import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.StationCatalog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StationStatusCalculator {
    private StationStatusCalculator() {
    }

    public static Map<String, MetricStatus> systemStatuses(
            Map<String, MeasurementSnapshot> measurements,
            Map<String, String> availability,
            Instant now
    ) {
        Map<String, MetricStatus> statuses = new LinkedHashMap<>();
        for (var system : StationCatalog.SYSTEMS) {
            String systemId = system.id();
            List<MetricStatus> metricStatuses = system.metricKeys().stream()
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

    public static MetricStatus statusOf(MeasurementSnapshot measurement, Instant now) {
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
        return MetricStatusEvaluator.isStale(measuredAt, now)
                ? MetricStatus.UNKNOWN
                : status;
    }

    public static boolean isPhysicallyValid(MeasurementSnapshot measurement) {
        return measurement != null
                && MetricStatusEvaluator.isPhysicallyValid(
                        measurement.metricKey(),
                        measurement.value()
                )
                && measurement.unit().equals(
                        MetricStatusEvaluator.ruleFor(measurement.metricKey()).unit());
    }
}
