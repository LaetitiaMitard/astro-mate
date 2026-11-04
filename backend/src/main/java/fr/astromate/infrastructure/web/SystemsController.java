package fr.astromate.infrastructure.web;

import fr.astromate.application.service.MeasurementHistoryService;
import fr.astromate.application.service.SystemDetailService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class SystemsController {
    private final SystemDetailService systemDetailService;
    private final MeasurementHistoryService measurementHistoryService;

    SystemsController(
            SystemDetailService systemDetailService,
            MeasurementHistoryService measurementHistoryService
    ) {
        this.systemDetailService = systemDetailService;
        this.measurementHistoryService = measurementHistoryService;
    }

    @GetMapping("/systems")
    List<SystemSummaryResponse> getSystems() {
        return systemDetailService.getSystems().stream().map(WebResponses::from).toList();
    }

    @GetMapping("/systems/{systemId}")
    SystemDetailResponse getSystemDetail(@PathVariable String systemId) {
        return WebResponses.from(systemDetailService.getSystemDetail(systemId));
    }

    @GetMapping("/systems/{systemId}/indicators/{indicatorKey}/measurements")
    MeasurementSeriesResponse getIndicatorMeasurements(
            @PathVariable String systemId,
            @PathVariable String indicatorKey,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return WebResponses.from(measurementHistoryService.getMeasurements(
                systemId,
                indicatorKey,
                from,
                to
        ));
    }

}
