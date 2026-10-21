package fr.astromate;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
class DashboardController {
    private final DashboardService dashboardService;
    private final SystemDetailService systemDetailService;
    private final MeasurementHistoryService measurementHistoryService;
    private final EventLogService eventLogService;
    private final MeasurementChangeNotifier measurementChangeNotifier;

    DashboardController(
            DashboardService dashboardService,
            SystemDetailService systemDetailService,
            MeasurementHistoryService measurementHistoryService,
            EventLogService eventLogService,
            MeasurementChangeNotifier measurementChangeNotifier
    ) {
        this.dashboardService = dashboardService;
        this.systemDetailService = systemDetailService;
        this.measurementHistoryService = measurementHistoryService;
        this.eventLogService = eventLogService;
        this.measurementChangeNotifier = measurementChangeNotifier;
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

    @GetMapping("/events")
    EventLogResponse getEvents(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return eventLogService.getEvents(
                page,
                pageSize,
                from,
                to,
                level,
                category,
                q,
                sort
        );
    }

    @GetMapping(value = "/telemetry/changes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribeToMeasurementChanges() {
        SseEmitter emitter = new SseEmitter(0L);
        measurementChangeNotifier.subscribe(emitter);
        return emitter;
    }
}
