package fr.astromate.application.port;

import fr.astromate.application.model.MeasurementSeriesView.Measurement;
import java.time.Instant;
import java.util.List;

public interface HistoryQueries {
    List<Measurement> readMetricMeasurements(String metricKey, String unit, Instant from, Instant to);

    List<Measurement> readTemperatureMeasurements(
            List<String> metricKeys, String unit, Instant from, Instant to
    );
}
