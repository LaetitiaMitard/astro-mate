package fr.astromate;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class MeasurementFreshnessMonitor {
    private final MeasurementService measurementService;

    MeasurementFreshnessMonitor(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @Scheduled(fixedRate = 1_000)
    void checkForStaleMeasurements() {
        measurementService.recordStaleMeasurements(Instant.now());
    }
}
