package fr.astromate.infrastructure.persistence;

import fr.astromate.application.model.SystemDetailView;
import fr.astromate.application.port.SystemDetailQueries;
import fr.astromate.domain.model.StationCatalog;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSystemDetailQueries implements SystemDetailQueries {
    private final JdbcTemplate jdbcTemplate;

    public JdbcSystemDetailQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SystemDetailView.Event> recentEvents(String systemId) {
        return jdbcTemplate.query(
                """
                SELECT id, level, category, system_id, source, message, occurred_at
                FROM station_event
                WHERE system_id = ?
                ORDER BY occurred_at DESC
                LIMIT 20
                """,
                (resultSet, rowNumber) -> new SystemDetailView.Event(
                        resultSet.getString("id"),
                        resultSet.getString("level"),
                        resultSet.getString("category"),
                        resultSet.getString("system_id"),
                        StationCatalog.systemName(resultSet.getString("system_id")),
                        resultSet.getString("source"),
                        resultSet.getString("message"),
                        resultSet.getString("occurred_at")
                ),
                systemId
        );
    }

    @Override
    public Instant simulatedStateStartedAt() {
        return Instant.parse(jdbcTemplate.queryForObject(
                "SELECT initialized_at FROM simulation_state WHERE scenario_id = 'thermal'",
                String.class));
    }
}
