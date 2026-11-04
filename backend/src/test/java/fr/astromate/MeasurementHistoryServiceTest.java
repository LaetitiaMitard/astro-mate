package fr.astromate;

import fr.astromate.application.model.ApplicationException;
import fr.astromate.application.model.MeasurementSeriesView;
import fr.astromate.application.service.MeasurementHistoryService;
import fr.astromate.infrastructure.persistence.JdbcHistoryQueries;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasurementHistoryServiceTest {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private MeasurementHistoryService service;

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
        service = new MeasurementHistoryService(new JdbcHistoryQueries(jdbcTemplate), java.time.Clock.systemUTC());
    }

    @AfterEach
    void closeDataSource() {
        dataSource.destroy();
    }

    @Test
    void rejectsMalformedPeriodBounds() {
        assertThatThrownBy(() -> service.getMeasurements(
                "thermal-control",
                "temperature",
                "not-a-timestamp",
                null
        ))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.kind())
                                .isEqualTo(ApplicationException.Kind.BAD_REQUEST)
                );
    }

    @Test
    void rejectsReversedPeriodBounds() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> service.getMeasurements(
                "thermal-control",
                "temperature",
                now.toString(),
                now.minusSeconds(1).toString()
        ))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.kind())
                                .isEqualTo(ApplicationException.Kind.BAD_REQUEST)
                );
    }

    @Test
    void clampsHistoryToSixHoursAndTheCurrentTime() {
        Instant now = Instant.now();
        Instant outsideWindow = now.minus(Duration.ofHours(6)).minusSeconds(5);
        Instant inWindow = now.minus(Duration.ofHours(2));
        Instant future = now.plusSeconds(30);
        insertTemperaturePair(outsideWindow, 20, 20);
        insertTemperaturePair(inWindow, 21.4, 22.1);
        insertTemperaturePair(future, 24, 24);

        MeasurementSeriesView series = service.getMeasurements(
                "thermal-control",
                "temperature",
                null,
                null
        );

        assertThat(series.measurements()).containsExactly(
                new MeasurementSeriesView.Measurement(21.8, inWindow.toString())
        );
        assertThat(Duration.between(
                Instant.parse(series.from()),
                Instant.parse(series.to())
        )).isEqualTo(Duration.ofHours(6));
    }

    private void insertTemperaturePair(
            Instant measuredAt,
            double radiatorA,
            double radiatorB
    ) {
        insertMeasurement("radiatorATemperature", radiatorA, measuredAt);
        insertMeasurement("radiatorBTemperature", radiatorB, measuredAt);
    }

    private void insertMeasurement(String metricKey, double value, Instant measuredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO measurement_history (metric_key, value, unit, measured_at)
                VALUES (?, ?, '°C', ?)
                """,
                metricKey,
                value,
                measuredAt.toString()
        );
    }
}
