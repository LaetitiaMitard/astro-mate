package fr.astromate;

import java.util.List;

public record MeasurementSeriesResponse(
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
