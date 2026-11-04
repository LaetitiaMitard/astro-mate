package fr.astromate.infrastructure.scheduling;

import fr.astromate.application.service.DataRetentionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionScheduling {
    private final DataRetentionService service;

    public RetentionScheduling(DataRetentionService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void applyRetention() {
        service.applyRetention();
    }
}
