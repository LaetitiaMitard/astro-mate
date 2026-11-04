package fr.astromate.application.model;

import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import java.util.List;
import java.util.Map;

public record StationOverview(
        Map<String, MeasurementSnapshot> measurements,
        Map<String, String> availability,
        Map<String, MetricStatus> statuses,
        List<DashboardView.Indicator> indicators
) {
}
