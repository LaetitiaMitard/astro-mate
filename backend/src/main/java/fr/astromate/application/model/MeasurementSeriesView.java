package fr.astromate.application.model;

import java.util.List;

public record MeasurementSeriesView(
        String indicatorKey,
        List<String> sourceMetricIds,
        String name,
        String unit,
        String from,
        String to,
        List<Measurement> measurements
) {
    public record Measurement(double value, String measuredAt) {
    }
}
