package fr.astromate.infrastructure.web;

import fr.astromate.application.service.AlertLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class AlertsController {
    private final AlertLogService alertLogService;

    AlertsController(AlertLogService alertLogService) {
        this.alertLogService = alertLogService;
    }

    @GetMapping("/alerts")
    AlertPageResponse getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String systemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return WebResponses.from(alertLogService.getAlerts(page, pageSize, status, level, systemId));
    }

}
