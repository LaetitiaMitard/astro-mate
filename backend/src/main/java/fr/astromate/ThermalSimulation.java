package fr.astromate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ThermalSimulation {
    private final MeasurementService measurementService;
    private final Clock clock;
    private Instant startedAt;

    ThermalSimulation(MeasurementService measurementService, Clock clock) {
        this.measurementService = measurementService;
        this.clock = clock;
    }

    @Transactional
    public void produceTelemetry() {
        Instant measuredAt = clock.instant();
        Instant cycleStartedAt = startedAt == null ? measuredAt : startedAt;
        long phase = (Duration.between(cycleStartedAt, measuredAt).toSeconds() / 60) % 4;
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
        startedAt = cycleStartedAt;
    }
}
