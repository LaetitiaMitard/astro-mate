package fr.astromate.application.service;

import fr.astromate.application.port.SimulationRepository;
import fr.astromate.application.port.Transactions;
import fr.astromate.domain.service.ThermalScenario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class ThermalSimulation {
    private final MeasurementService measurementService;
    private final Clock clock;
    private final SimulationRepository repository;
    private final Transactions transactions;
    private Instant lastProducedAt;

    public ThermalSimulation(
            MeasurementService measurementService, Clock clock,
            SimulationRepository repository, Transactions transactions
    ) {
        this.measurementService = measurementService;
        this.clock = clock;
        this.repository = repository;
        this.transactions = transactions;
    }

    public void produceTelemetry() {
        transactions.execute(() -> {
            Instant measuredAt = clock.instant();
            long savedElapsedMillis = repository.elapsedMillis();
            if (lastProducedAt != null && measuredAt.isBefore(lastProducedAt)) {
                throw new IllegalStateException("L’horloge de simulation a reculé.");
            }
            long elapsedSinceLastProduction = lastProducedAt == null
                    ? 0 : Duration.between(lastProducedAt, measuredAt).toMillis();
            long elapsedMillis = (savedElapsedMillis
                    + elapsedSinceLastProduction % ThermalScenario.CYCLE_MILLIS)
                    % ThermalScenario.CYCLE_MILLIS;
            var temperatures = ThermalScenario.temperatures(elapsedMillis);

            measurementService.recordMeasurement("radiatorATemperature", temperatures.radiatorA(), measuredAt);
            measurementService.recordMeasurement("radiatorBTemperature", temperatures.radiatorB(), measuredAt);
            measurementService.recordMeasurement("oxygen", 21, measuredAt);
            measurementService.recordMeasurement("carbonDioxide", 400, measuredAt);
            measurementService.recordMeasurement("availableEnergy", 78, measuredAt);
            measurementService.recordMeasurement("fluidFlow", 120, measuredAt);
            measurementService.recordMeasurement("pressure", 2.1, measuredAt);
            measurementService.recordMeasurement("thermalConsumption", 1.2, measuredAt);
            repository.saveElapsedMillis(elapsedMillis);
            transactions.afterCommit(() -> lastProducedAt = measuredAt);
        });
    }
}
