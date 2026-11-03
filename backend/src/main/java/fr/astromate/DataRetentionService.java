package fr.astromate;

import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DataRetentionService {
    private static final Duration MEASUREMENT_RETENTION = Duration.ofHours(6);
    private static final Duration EVENT_RETENTION = Duration.ofDays(7);
    private static final long CLEANUP_INTERVAL_MILLIS = 60_000;

    private final JdbcTemplate jdbcTemplate;

    DataRetentionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(
            fixedDelay = CLEANUP_INTERVAL_MILLIS,
            initialDelay = CLEANUP_INTERVAL_MILLIS
    )
    @Transactional
    public void applyRetention() {
        applyRetentionAt(Instant.now());
    }

    void applyRetentionAt(Instant now) {
        Instant measurementCutoff = now.minus(MEASUREMENT_RETENTION);
        Instant eventCutoff = now.minus(EVENT_RETENTION);

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
