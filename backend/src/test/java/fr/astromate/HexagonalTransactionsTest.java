package fr.astromate;

import fr.astromate.application.port.Transactions;
import fr.astromate.application.service.MeasurementService;
import fr.astromate.application.service.ThermalSimulation;
import fr.astromate.infrastructure.web.MeasurementChangeNotifier;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "astromate.simulation.enabled=false",
        "spring.application.name=hexagonal-transactions"
})
class HexagonalTransactionsTest {
    @Autowired
    private Transactions transactions;
    @Autowired
    private MeasurementService measurements;
    @Autowired
    private ThermalSimulation simulation;
    @Autowired
    private MeasurementChangeNotifier notifier;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private Clock clock;

    @Test
    void rollsBackMeasurementHistoryAlertsAndEventsWithoutNotifying() throws Exception {
        var before = jdbc.queryForMap("SELECT * FROM metric_measurement WHERE metric_key = 'oxygen'");
        long historyCount = count("measurement_history");
        long eventCount = count("station_event");
        long alertCount = count("alert_episode");
        SseEmitter subscriber = mock(SseEmitter.class);
        notifier.subscribe(subscriber);
        Instant next = Instant.parse((String) before.get("measured_at")).plusSeconds(1);
        try {
            assertThatThrownBy(() -> transactions.execute(() -> {
                measurements.recordMeasurement("oxygen", 16, next);
                assertThat(jdbc.queryForObject(
                        "SELECT value FROM metric_measurement WHERE metric_key = 'oxygen'", Double.class))
                        .isEqualTo(16);
                throw new IllegalStateException("rollback");
            })).isInstanceOf(IllegalStateException.class).hasMessage("rollback");

            assertThat(jdbc.queryForMap("SELECT * FROM metric_measurement WHERE metric_key = 'oxygen'"))
                    .isEqualTo(before);
            assertThat(count("measurement_history")).isEqualTo(historyCount);
            assertThat(count("station_event")).isEqualTo(eventCount);
            assertThat(count("alert_episode")).isEqualTo(alertCount);
            verify(subscriber, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        } finally {
            unsubscribe(subscriber);
        }
    }

    @Test
    void notifiesOnlyAfterTheOuterTransactionCommits() throws Exception {
        SseEmitter subscriber = mock(SseEmitter.class);
        notifier.subscribe(subscriber);
        String timestamp = jdbc.queryForObject(
                "SELECT measured_at FROM metric_measurement WHERE metric_key = 'carbonDioxide'",
                String.class);
        try {
            transactions.execute(() -> {
                measurements.recordMeasurement("carbonDioxide", 500, Instant.parse(timestamp).plusSeconds(1));
                try {
                    verify(subscriber, times(1)).send(any(SseEmitter.SseEventBuilder.class));
                } catch (java.io.IOException exception) {
                    throw new AssertionError(exception);
                }
            });
            verify(subscriber, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        } finally {
            unsubscribe(subscriber);
        }
    }

    @Test
    void failedSimulationRollsBackItsEntireBatchAndDoesNotAdvanceTheInMemoryClock() {
        Instant startedAt = jdbc.queryForList("SELECT measured_at FROM metric_measurement", String.class)
                .stream().map(Instant::parse).max(Instant::compareTo).orElseThrow().plusSeconds(1);
        when(clock.instant()).thenReturn(startedAt);
        simulation.produceTelemetry();
        var before = jdbc.queryForList("SELECT * FROM metric_measurement ORDER BY metric_key");
        long historyCount = count("measurement_history");
        long eventCount = count("station_event");
        long savedElapsed = jdbc.queryForObject(
                "SELECT elapsed_millis FROM simulation_state WHERE scenario_id = 'thermal'", Long.class);
        jdbc.execute("""
                CREATE TEMP TRIGGER fail_simulation_state BEFORE UPDATE ON simulation_state
                BEGIN
                    SELECT RAISE(ABORT, 'simulation write failed');
                END
                """);
        try {
            when(clock.instant()).thenReturn(startedAt.plusSeconds(50));
            assertThatThrownBy(simulation::produceTelemetry).isInstanceOf(DataAccessException.class);
            assertThat(jdbc.queryForList("SELECT * FROM metric_measurement ORDER BY metric_key"))
                    .isEqualTo(before);
            assertThat(count("measurement_history")).isEqualTo(historyCount);
            assertThat(count("station_event")).isEqualTo(eventCount);
            assertThat(jdbc.queryForObject(
                    "SELECT elapsed_millis FROM simulation_state WHERE scenario_id = 'thermal'", Long.class))
                    .isEqualTo(savedElapsed);
        } finally {
            jdbc.execute("DROP TRIGGER fail_simulation_state");
        }
        when(clock.instant()).thenReturn(startedAt.plusSeconds(60));
        simulation.produceTelemetry();
        assertThat(jdbc.queryForObject(
                "SELECT elapsed_millis FROM simulation_state WHERE scenario_id = 'thermal'", Long.class))
                .isEqualTo((savedElapsed + 60_000) % 240_000);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private void unsubscribe(SseEmitter subscriber) {
        var completion = ArgumentCaptor.forClass(Runnable.class);
        verify(subscriber).onCompletion(completion.capture());
        completion.getValue().run();
    }
}
