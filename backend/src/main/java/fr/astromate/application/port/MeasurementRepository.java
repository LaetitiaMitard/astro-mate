package fr.astromate.application.port;

import fr.astromate.domain.model.AlertEpisode;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.TechnicalEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository {
    Optional<MeasurementSnapshot> findMeasurement(String metricKey);

    String availability(String systemId);

    List<MeasurementSnapshot> onlineMeasurements();

    void saveMeasurement(String metricKey, double value, Instant measuredAt);

    void clearStaleness(String metricKey);

    boolean startStaleness(String metricKey, Instant startedAt);

    boolean startInvalidData(String metricKey, Instant startedAt);

    boolean closeInvalidData(String metricKey);

    Optional<AlertEpisode> openAlert(String metricKey);

    void resolveAlert(String id, Instant resolvedAt);

    void createAlert(MeasurementSnapshot metric, MetricStatus level, String message, Instant detectedAt);

    void appendEvent(TechnicalEvent event);

    boolean changeAvailability(String systemId, String availability);

    void updateCompatibilityProjection(String metricKey, double value, Instant measuredAt);
}
