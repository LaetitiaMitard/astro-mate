package fr.astromate;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MeasurementChangeNotifierTest {
    @Test
    void notifiesConnectedClientsWhenMeasurementsChange() throws IOException {
        MeasurementChangeNotifier notifier = new MeasurementChangeNotifier();
        SseEmitter subscriber = mock(SseEmitter.class);
        notifier.subscribe(subscriber);

        notifier.measurementsChanged();

        verify(subscriber, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
