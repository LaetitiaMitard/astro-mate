package fr.astromate.infrastructure.configuration;

import fr.astromate.application.port.AlertQueries;
import fr.astromate.application.port.EventQueries;
import fr.astromate.application.port.HistoryQueries;
import fr.astromate.application.port.MeasurementChanges;
import fr.astromate.application.port.MeasurementRepository;
import fr.astromate.application.port.RetentionRepository;
import fr.astromate.application.port.SimulationRepository;
import fr.astromate.application.port.StationRepository;
import fr.astromate.application.port.SystemDetailQueries;
import fr.astromate.application.port.Transactions;
import fr.astromate.application.service.AlertLogService;
import fr.astromate.application.service.DashboardService;
import fr.astromate.application.service.DataRetentionService;
import fr.astromate.application.service.EventLogService;
import fr.astromate.application.service.MeasurementHistoryService;
import fr.astromate.application.service.MeasurementService;
import fr.astromate.application.service.StationOverviewService;
import fr.astromate.application.service.SystemDetailService;
import fr.astromate.application.service.ThermalSimulation;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    Clock simulationClock() {
        return Clock.systemUTC();
    }

    @Bean
    MeasurementService measurementService(
            MeasurementRepository repository, MeasurementChanges changes, Transactions transactions
    ) {
        return new MeasurementService(repository, changes, transactions, Clock.systemUTC());
    }

    @Bean
    StationOverviewService stationOverviewService(StationRepository repository) {
        return new StationOverviewService(repository, Clock.systemUTC());
    }

    @Bean
    DashboardService dashboardService(StationOverviewService overview, AlertQueries queries) {
        return new DashboardService(overview, queries);
    }

    @Bean
    SystemDetailService systemDetailService(StationOverviewService overview, SystemDetailQueries queries) {
        return new SystemDetailService(overview, queries, Clock.systemUTC());
    }

    @Bean
    MeasurementHistoryService measurementHistoryService(HistoryQueries queries) {
        return new MeasurementHistoryService(queries, Clock.systemUTC());
    }

    @Bean
    AlertLogService alertLogService(AlertQueries queries) {
        return new AlertLogService(queries);
    }

    @Bean
    EventLogService eventLogService(EventQueries queries) {
        return new EventLogService(queries, Clock.systemUTC());
    }

    @Bean
    ThermalSimulation thermalSimulation(
            MeasurementService measurements, Clock simulationClock,
            SimulationRepository repository, Transactions transactions
    ) {
        return new ThermalSimulation(measurements, simulationClock, repository, transactions);
    }

    @Bean
    DataRetentionService dataRetentionService(RetentionRepository repository, Transactions transactions) {
        return new DataRetentionService(repository, transactions, Clock.systemUTC());
    }
}
