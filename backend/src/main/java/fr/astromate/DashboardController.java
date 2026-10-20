package fr.astromate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class DashboardController {
    private final DashboardService dashboardService;
    private final SystemDetailService systemDetailService;

    DashboardController(
            DashboardService dashboardService,
            SystemDetailService systemDetailService
    ) {
        this.dashboardService = dashboardService;
        this.systemDetailService = systemDetailService;
    }

    @GetMapping("/dashboard")
    DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/systems/{systemId}")
    SystemDetailResponse getSystemDetail(@PathVariable String systemId) {
        return systemDetailService.getSystemDetail(systemId);
    }
}
