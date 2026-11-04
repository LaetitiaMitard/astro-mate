package fr.astromate.infrastructure.web;

import fr.astromate.application.service.EventLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class EventsController {
    private final EventLogService eventLogService;

    EventsController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
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
        return WebResponses.from(eventLogService.getEvents(
                page,
                pageSize,
                from,
                to,
                level,
                category,
                q,
                sort
        ));
    }

}
