package fr.astromate;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
class MeasurementChangeNotifier {
    private final CopyOnWriteArrayList<SseEmitter> subscribers =
            new CopyOnWriteArrayList<>();

    void subscribe(SseEmitter subscriber) {
        subscribers.add(subscriber);
        subscriber.onCompletion(() -> subscribers.remove(subscriber));
        subscriber.onTimeout(() -> subscribers.remove(subscriber));
        subscriber.onError(error -> subscribers.remove(subscriber));
        sendUpdate(subscriber);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onMeasurementChanged(MeasurementChangeEvent event) {
        measurementsChanged();
    }

    void measurementsChanged() {
        subscribers.forEach(this::sendUpdate);
    }

    private void sendUpdate(SseEmitter subscriber) {
        try {
            subscriber.send(SseEmitter.event().data("updated"));
        } catch (IOException exception) {
            subscribers.remove(subscriber);
        }
    }
}
