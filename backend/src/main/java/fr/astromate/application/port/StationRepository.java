package fr.astromate.application.port;

import fr.astromate.domain.model.MeasurementSnapshot;
import java.util.Map;

public interface StationRepository {
    Map<String, MeasurementSnapshot> readMeasurements();

    Map<String, String> readAvailability();
}
