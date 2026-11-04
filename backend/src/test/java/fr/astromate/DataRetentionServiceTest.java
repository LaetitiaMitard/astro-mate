package fr.astromate;

import fr.astromate.application.service.DataRetentionService;
import fr.astromate.infrastructure.configuration.SpringTransactions;
import fr.astromate.infrastructure.persistence.JdbcRetentionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DataRetentionServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");

    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private DataRetentionService service;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                """
                CREATE TABLE measurement_history (
                    metric_key TEXT NOT NULL,
                    value REAL NOT NULL,
                    unit TEXT NOT NULL,
                    measured_at TEXT NOT NULL,
                    PRIMARY KEY (metric_key, measured_at)
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE alert_episode (
                    id TEXT PRIMARY KEY,
                    metric_key TEXT NOT NULL,
                    status TEXT NOT NULL,
                    detected_at TEXT NOT NULL,
                    resolved_at TEXT
                )
                """
        );
        jdbcTemplate.execute(
                """
                CREATE TABLE station_event (
                    id TEXT PRIMARY KEY,
                    category TEXT NOT NULL,
                    source TEXT,
                    occurred_at TEXT NOT NULL
                )
                """
        );
        service = new DataRetentionService(
                new JdbcRetentionRepository(jdbcTemplate),
                new SpringTransactions(new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource)),
                java.time.Clock.systemUTC());
    }

    @AfterEach
    void closeDataSource() {
        dataSource.destroy();
    }

    @Test
    void retainsMeasurementHistoryAtTheSixHourBoundary() {
        insertMeasurement("inside", NOW.minusSeconds(6 * 60 * 60 - 1));
        insertMeasurement("boundary", NOW.minusSeconds(6 * 60 * 60));
        insertMeasurement("expired", NOW.minusSeconds(6 * 60 * 60 + 1));

        service.applyRetentionAt(NOW);

        assertThat(readMeasurementKeys()).containsExactlyInAnyOrder("inside", "boundary");
    }

    @Test
    void retainsRecentAndAlertProtectedDataWhileRemovingExpiredData() {
        insertAlert("open", "oxygen", "OPEN", NOW.minusSeconds(10 * 24 * 60 * 60), null);
        insertAlert(
                "resolved-recent",
                "carbonDioxide",
                "RESOLVED",
                NOW.minusSeconds(20 * 24 * 60 * 60),
                NOW.minusSeconds(6 * 24 * 60 * 60)
        );
        insertAlert(
                "resolved-boundary",
                "radiatorATemperature",
                "RESOLVED",
                NOW.minusSeconds(20 * 24 * 60 * 60),
                NOW.minusSeconds(7 * 24 * 60 * 60)
        );
        insertAlert(
                "resolved-expired",
                "radiatorBTemperature",
                "RESOLVED",
                NOW.minusSeconds(20 * 24 * 60 * 60),
                NOW.minusSeconds(7 * 24 * 60 * 60 + 1)
        );

        insertEvent("recent", "SYSTEM", "simulation", NOW.minusSeconds(6 * 24 * 60 * 60));
        insertEvent("boundary", "SYSTEM", "simulation", NOW.minusSeconds(7 * 24 * 60 * 60));
        insertEvent(
                "expired",
                "SYSTEM",
                "simulation",
                NOW.minusSeconds(7 * 24 * 60 * 60 + 1)
        );
        insertEvent(
                "open-alert",
                "ALERT",
                "oxygen",
                NOW.minusSeconds(9 * 24 * 60 * 60)
        );
        insertEvent(
                "before-open-alert",
                "ALERT",
                "oxygen",
                NOW.minusSeconds(20 * 24 * 60 * 60)
        );
        insertEvent(
                "recently-resolved-alert",
                "ALERT",
                "carbonDioxide",
                NOW.minusSeconds(20 * 24 * 60 * 60)
        );
        insertEvent(
                "expired-resolved-alert",
                "ALERT",
                "radiatorBTemperature",
                NOW.minusSeconds(20 * 24 * 60 * 60)
        );

        service.applyRetentionAt(NOW);

        assertThat(readAlertIds()).containsExactlyInAnyOrder(
                "open",
                "resolved-recent",
                "resolved-boundary"
        );
        assertThat(readEventIds()).containsExactlyInAnyOrder(
                "recent",
                "boundary",
                "open-alert",
                "recently-resolved-alert"
        );
    }

    private void insertMeasurement(String metricKey, Instant measuredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO measurement_history (metric_key, value, unit, measured_at)
                VALUES (?, 21, '% vol.', ?)
                """,
                metricKey,
                measuredAt.toString()
        );
    }

    private void insertAlert(
            String id,
            String metricKey,
            String status,
            Instant detectedAt,
            Instant resolvedAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO alert_episode
                    (id, metric_key, status, detected_at, resolved_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                id,
                metricKey,
                status,
                detectedAt.toString(),
                resolvedAt == null ? null : resolvedAt.toString()
        );
    }

    private void insertEvent(
            String id,
            String category,
            String source,
            Instant occurredAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO station_event (id, category, source, occurred_at)
                VALUES (?, ?, ?, ?)
                """,
                id,
                category,
                source,
                occurredAt.toString()
        );
    }

    private List<String> readMeasurementKeys() {
        return jdbcTemplate.queryForList(
                "SELECT metric_key FROM measurement_history",
                String.class
        );
    }

    private List<String> readAlertIds() {
        return jdbcTemplate.queryForList("SELECT id FROM alert_episode", String.class);
    }

    private List<String> readEventIds() {
        return jdbcTemplate.queryForList("SELECT id FROM station_event", String.class);
    }
}
