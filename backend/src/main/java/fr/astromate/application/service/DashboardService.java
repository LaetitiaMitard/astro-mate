package fr.astromate.application.service;

import fr.astromate.application.model.DashboardView;
import fr.astromate.application.model.DashboardView.SystemState;
import fr.astromate.application.model.StationOverview;
import fr.astromate.application.port.AlertQueries;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.StationCatalog;
import fr.astromate.domain.service.MetricStatusEvaluator;
import fr.astromate.domain.service.StationStatusCalculator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DashboardService {
    private static final List<String> SYSTEM_IDS = StationCatalog.SYSTEMS.stream()
            .map(StationCatalog.SystemMetadata::id).toList();
    private final StationOverviewService overviewService;
    private final AlertQueries queries;

    public DashboardService(StationOverviewService overviewService, AlertQueries queries) {
        this.overviewService = overviewService;
        this.queries = queries;
    }

    public DashboardView getDashboard() {
        StationOverview overview = overviewService.getOverview();
        var measurements = overview.measurements();
        var availability = overview.availability();
        var indicators = overview.indicators();
        var systemStatuses = overview.statuses();
        MetricStatus stationStatus = MetricStatusEvaluator.mostSevere(
                new ArrayList<>(systemStatuses.values())
        );
        List<String> offlineSystems = SYSTEM_IDS.stream()
                .filter(systemId -> "OFFLINE".equals(availability.get(systemId)))
                .toList();
        List<SystemState> systemStateSummaries = SYSTEM_IDS.stream()
                .map(systemId -> new SystemState(
                        systemId,
                        systemStatuses.get(systemId).name()
                ))
                .toList();
        String lastMeasurementAt = measurements.values().stream()
                .filter(measurement -> StationStatusCalculator.isPhysicallyValid(measurement))
                .max(Comparator.comparing(
                        measurement -> Instant.parse(measurement.measuredAt())
                ))
                .map(MeasurementSnapshot::measuredAt)
                .orElse(null);

        return new DashboardView(
                stationStatus.name(),
                offlineSystems,
                systemStateSummaries,
                "ACTIVE",
                lastMeasurementAt,
                indicators,
                queries.recentOpenAlerts()
        );
    }
}
