package fr.astromate.infrastructure.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
class TelemetryController {
    private final MeasurementChangeNotifier measurementChangeNotifier;

    TelemetryController(MeasurementChangeNotifier measurementChangeNotifier) {
        this.measurementChangeNotifier = measurementChangeNotifier;
    }

    @GetMapping(value = "/telemetry/changes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribeToMeasurementChanges() {
        SseEmitter emitter = new SseEmitter(0L);
        measurementChangeNotifier.subscribe(emitter);
        return emitter;
    }

}
