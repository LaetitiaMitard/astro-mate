package fr.astromate;

import java.util.List;

public record DashboardResponse(
        String stationStatus,
        List<String> offlineSystems,
        List<SystemState> systemStatuses,
        String simulationStatus,
        String lastMeasurementAt,
        List<Indicator> indicators,
        List<Alert> recentOpenAlerts
) {
    public record SystemState(String systemId, String status) {
    }

    public record Indicator(
            String key,
            String systemId,
            List<String> sourceMetricIds,
            String name,
            Double value,
            String unit,
            String status,
            Range nominalRange,
            String measuredAt
    ) {
    }

    public record Range(Double min, Double max, boolean minInclusive, boolean maxInclusive) {
    }

    public record Alert(
            String id,
            String level,
            String systemId,
            String systemName,
            String metricId,
            String message,
            String status,
            String detectedAt,
            String resolvedAt
    ) {
    }
}
