package fr.astromate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
class ThermalSimulation {
    private static final long CYCLE_MILLIS = 240_000;
    private final MeasurementService measurementService;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;
    private Instant lastProducedAt;

    ThermalSimulation(
            MeasurementService measurementService,
            Clock clock,
            JdbcTemplate jdbcTemplate
    ) {
        this.measurementService = measurementService;
        this.clock = clock;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void produceTelemetry() {
        Instant measuredAt = clock.instant();
        long savedElapsedMillis = jdbcTemplate.queryForObject(
                "SELECT elapsed_millis FROM simulation_state WHERE scenario_id = 'thermal'",
                Long.class
        );
        if (lastProducedAt != null && measuredAt.isBefore(lastProducedAt)) {
            throw new IllegalStateException("L’horloge de simulation a reculé.");
        }
        long elapsedSinceLastProduction = lastProducedAt == null
                ? 0
                : Duration.between(lastProducedAt, measuredAt).toMillis();
        long elapsedMillis = (savedElapsedMillis
                + elapsedSinceLastProduction % CYCLE_MILLIS) % CYCLE_MILLIS;
        long phase = elapsedMillis / 60_000;
        double radiatorA = phase == 1 ? 27 : phase == 2 ? 31 : 22.1;
        double radiatorB = phase == 1 ? 27 : phase == 2 ? 31 : 22.4;

        measurementService.recordMeasurement("radiatorATemperature", radiatorA, measuredAt);
        measurementService.recordMeasurement("radiatorBTemperature", radiatorB, measuredAt);
        measurementService.recordMeasurement("oxygen", 21, measuredAt);
        measurementService.recordMeasurement("carbonDioxide", 400, measuredAt);
        measurementService.recordMeasurement("availableEnergy", 78, measuredAt);
        measurementService.recordMeasurement("fluidFlow", 120, measuredAt);
        measurementService.recordMeasurement("pressure", 2.1, measuredAt);
        measurementService.recordMeasurement("thermalConsumption", 1.2, measuredAt);
        jdbcTemplate.update(
                "UPDATE simulation_state SET elapsed_millis = ? WHERE scenario_id = 'thermal'",
                elapsedMillis
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                lastProducedAt = measuredAt;
            }
        });
    }
}
