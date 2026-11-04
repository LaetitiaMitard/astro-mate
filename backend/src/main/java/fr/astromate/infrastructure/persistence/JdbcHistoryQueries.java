package fr.astromate.infrastructure.persistence;

import fr.astromate.application.model.MeasurementSeriesView.Measurement;
import fr.astromate.application.port.HistoryQueries;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcHistoryQueries implements HistoryQueries {
    private final JdbcTemplate jdbcTemplate;

    public JdbcHistoryQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Measurement> readMetricMeasurements(
            String metricKey,
            String unit,
            Instant from,
            Instant to
    ) {
        return jdbcTemplate.query(
                """
                SELECT value, measured_at
                FROM measurement_history
                WHERE metric_key = ?
                  AND unit = ?
                  AND julianday(measured_at) >= julianday(?)
                  AND julianday(measured_at) < julianday(?)
                ORDER BY julianday(measured_at), measured_at
                """,
                (resultSet, rowNumber) -> new Measurement(
                        resultSet.getDouble("value"),
                        resultSet.getString("measured_at")
                ),
                metricKey,
                unit,
                from.toString(),
                to.toString()
        );
    }

    @Override
    public List<Measurement> readTemperatureMeasurements(
            List<String> metricKeys,
            String unit,
            Instant from,
            Instant to
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    (radiator_a.value / 2.0 + radiator_b.value / 2.0) AS value,
                    radiator_a.measured_at AS measured_at
                FROM measurement_history radiator_a
                JOIN measurement_history radiator_b
                  ON radiator_b.metric_key = ?
                 AND radiator_b.measured_at = radiator_a.measured_at
                WHERE radiator_a.metric_key = ?
                  AND radiator_a.unit = ?
                  AND radiator_b.unit = ?
                  AND julianday(radiator_a.measured_at) >= julianday(?)
                  AND julianday(radiator_a.measured_at) < julianday(?)
                ORDER BY julianday(radiator_a.measured_at), radiator_a.measured_at
                """,
                (resultSet, rowNumber) -> new Measurement(
                        BigDecimal.valueOf(resultSet.getDouble("value"))
                                .setScale(1, RoundingMode.HALF_UP)
                                .doubleValue(),
                        resultSet.getString("measured_at")
                ),
                metricKeys.get(1),
                metricKeys.get(0),
                unit,
                unit,
                from.toString(),
                to.toString()
        );
    }
}
