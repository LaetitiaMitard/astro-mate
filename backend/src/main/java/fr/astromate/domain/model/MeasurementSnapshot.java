package fr.astromate.domain.model;

public record MeasurementSnapshot(
        String metricKey, String systemId, String metricId, String name,
        double value, String unit, String measuredAt
) {
}
