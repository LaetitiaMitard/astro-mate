package fr.astromate;

import java.util.List;

public record SystemDetailResponse(
        String id,
        String name,
        String description,
        String status,
        String availability,
        String updatedAt,
        List<DashboardResponse.Indicator> indicators,
        List<Metric> metrics,
        List<Component> components,
        List<Event> recentEvents
) {
    public record Metric(
            String id,
            String name,
            String unit,
            Double value,
            String status,
            DashboardResponse.Range nominalRange,
            CriticalLimits criticalLimits,
            String measuredAt,
            String componentId
    ) {
    }

    public record CriticalLimits(Double minExclusive, Double maxExclusive) {
    }

    public record Component(
            String id,
            String name,
            String status,
            Double value,
            String unit,
            String updatedAt,
            boolean simulated
    ) {
    }

    public record Event(
            String id,
            String level,
            String category,
            String systemId,
            String systemName,
            String source,
            String message,
            String occurredAt
    ) {
    }
}
