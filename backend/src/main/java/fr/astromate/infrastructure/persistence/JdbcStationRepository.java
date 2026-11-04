package fr.astromate.infrastructure.persistence;

import fr.astromate.application.port.StationRepository;
import fr.astromate.domain.model.MeasurementSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcStationRepository implements StationRepository {
    private final JdbcTemplate jdbc;

    public JdbcStationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, MeasurementSnapshot> readMeasurements() {
        Map<String, MeasurementSnapshot> measurements = new LinkedHashMap<>();
        jdbc.query("""
                SELECT metric_key, system_id, metric_id, name, value, unit, measured_at
                FROM metric_measurement
                """, JdbcStationRepository::mapMeasurement)
                .forEach(row -> measurements.put(row.metricKey(), row));
        return measurements;
    }

    @Override
    public Map<String, String> readAvailability() {
        Map<String, String> availability = new LinkedHashMap<>();
        jdbc.query("SELECT system_id, availability FROM system_availability",
                row -> {
                    availability.put(row.getString("system_id"), row.getString("availability"));
                });
        return availability;
    }

    static MeasurementSnapshot mapMeasurement(ResultSet row, int rowNumber) throws SQLException {
        return new MeasurementSnapshot(
                row.getString("metric_key"), row.getString("system_id"),
                row.getString("metric_id"), row.getString("name"), row.getDouble("value"),
                row.getString("unit"), row.getString("measured_at"));
    }
}
