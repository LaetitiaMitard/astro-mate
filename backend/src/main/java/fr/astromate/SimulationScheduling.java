package fr.astromate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "astromate.simulation.enabled",
        havingValue = "true",
        matchIfMissing = true
)
class SimulationScheduling {
    private final ThermalSimulation simulation;

    SimulationScheduling(ThermalSimulation simulation) {
        this.simulation = simulation;
    }

    @Scheduled(fixedRate = 10_000)
    void produceTelemetry() {
        simulation.produceTelemetry();
    }
}
