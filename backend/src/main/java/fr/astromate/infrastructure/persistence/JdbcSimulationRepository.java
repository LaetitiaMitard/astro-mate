package fr.astromate.infrastructure.persistence;

import fr.astromate.application.port.SimulationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSimulationRepository implements SimulationRepository {
    private final JdbcTemplate jdbc;

    public JdbcSimulationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long elapsedMillis() {
        return jdbc.queryForObject(
                "SELECT elapsed_millis FROM simulation_state WHERE scenario_id = 'thermal'",
                Long.class);
    }

    @Override
    public void saveElapsedMillis(long elapsedMillis) {
        jdbc.update("UPDATE simulation_state SET elapsed_millis = ? WHERE scenario_id = 'thermal'",
                elapsedMillis);
    }
}
