package fr.astromate.infrastructure.notification;

import fr.astromate.application.port.MeasurementChanges;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringMeasurementChanges implements MeasurementChanges {
    private final ApplicationEventPublisher publisher;

    public SpringMeasurementChanges(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish() {
        publisher.publishEvent(new MeasurementChangeEvent());
    }
}
