package fr.astromate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class DashboardController {
    private final DashboardService dashboardService;
    private final SystemDetailService systemDetailService;
    private final MeasurementHistoryService measurementHistoryService;

    DashboardController(
            DashboardService dashboardService,
            SystemDetailService systemDetailService,
            MeasurementHistoryService measurementHistoryService
    ) {
        this.dashboardService = dashboardService;
        this.systemDetailService = systemDetailService;
        this.measurementHistoryService = measurementHistoryService;
    }

    @GetMapping("/dashboard")
    DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/systems/{systemId}")
    SystemDetailResponse getSystemDetail(@PathVariable String systemId) {
        return systemDetailService.getSystemDetail(systemId);
    }

    @GetMapping("/systems/{systemId}/indicators/{indicatorKey}/measurements")
    MeasurementSeriesResponse getIndicatorMeasurements(
            @PathVariable String systemId,
            @PathVariable String indicatorKey,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return measurementHistoryService.getMeasurements(
                systemId,
                indicatorKey,
                from,
                to
        );
    }
}
