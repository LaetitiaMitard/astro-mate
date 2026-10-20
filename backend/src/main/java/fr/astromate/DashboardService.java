package fr.astromate;

import fr.astromate.DashboardResponse.Indicator;
import fr.astromate.DashboardResponse.Range;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class DashboardService {
    private static final int EXPECTED_INDICATOR_COUNT = 4;

    private final JdbcTemplate jdbcTemplate;

    DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    DashboardResponse getDashboard() {
        List<Indicator> indicators = jdbcTemplate.query(
                """
                SELECT indicator_key, system_id, metric_id, name, value, unit,
                       nominal_min, nominal_max, measured_at
                FROM dashboard_indicator
                ORDER BY display_order
                """,
                (resultSet, rowNumber) -> {
                    double value = resultSet.getDouble("value");
                    double nominalMin = resultSet.getDouble("nominal_min");
                    double nominalMax = resultSet.getDouble("nominal_max");
                    return new Indicator(
                            resultSet.getString("indicator_key"),
                            resultSet.getString("system_id"),
                            List.of(resultSet.getString("metric_id")),
                            resultSet.getString("name"),
                            value,
                            resultSet.getString("unit"),
                            statusFor(value, nominalMin, nominalMax),
                            new Range(nominalMin, nominalMax, true, true),
                            resultSet.getString("measured_at")
                    );
                }
        );

        if (indicators.size() != EXPECTED_INDICATOR_COUNT) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Les données du tableau de bord sont incomplètes."
            );
        }

        String stationStatus = indicators.stream()
                .map(Indicator::status)
                .filter(status -> status.equals("WARNING") || status.equals("CRITICAL"))
                .max((left, right) -> Integer.compare(severity(left), severity(right)))
                .orElse("NOMINAL");
        String lastMeasurementAt = indicators.stream()
                .map(Indicator::measuredAt)
                .max(String::compareTo)
                .orElse(null);

        return new DashboardResponse(
                stationStatus,
                List.of(),
                "ACTIVE",
                lastMeasurementAt,
                indicators,
                List.of()
        );
    }

    private static String statusFor(double value, double nominalMin, double nominalMax) {
        return value >= nominalMin && value <= nominalMax ? "NOMINAL" : "WARNING";
    }

    private static int severity(String status) {
        return status.equals("CRITICAL") ? 2 : 1;
    }
}
