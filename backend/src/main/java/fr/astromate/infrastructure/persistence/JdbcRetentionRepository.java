package fr.astromate.infrastructure.persistence;

import fr.astromate.application.port.RetentionRepository;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRetentionRepository implements RetentionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcRetentionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void deleteExpired(Instant measurementCutoff, Instant eventCutoff) {
        jdbcTemplate.update(
                """
                DELETE FROM measurement_history
                WHERE julianday(measured_at) < julianday(?)
                """,
                measurementCutoff.toString()
        );
        jdbcTemplate.update(
                """
                DELETE FROM station_event
                WHERE julianday(occurred_at) < julianday(?)
                  AND NOT (
                      category = 'ALERT'
                      AND EXISTS (
                          SELECT 1
                          FROM alert_episode
                          WHERE alert_episode.metric_key = station_event.source
                            AND julianday(station_event.occurred_at)
                                >= julianday(alert_episode.detected_at)
                            AND (
                                alert_episode.status = 'OPEN'
                                OR (
                                    alert_episode.status = 'RESOLVED'
                                    AND alert_episode.resolved_at IS NOT NULL
                                    AND julianday(station_event.occurred_at)
                                        <= julianday(alert_episode.resolved_at)
                                    AND julianday(alert_episode.resolved_at)
                                        >= julianday(?)
                                )
                            )
                      )
                  )
                """,
                eventCutoff.toString(),
                eventCutoff.toString()
        );
        jdbcTemplate.update(
                """
                DELETE FROM alert_episode
                WHERE status = 'RESOLVED'
                  AND resolved_at IS NOT NULL
                  AND julianday(resolved_at) < julianday(?)
                """,
                eventCutoff.toString()
        );
    }
}
