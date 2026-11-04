package fr.astromate.domain;

import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.service.StationStatusCalculator;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StationStatusCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");

    @Test
    void missingOrStaleMeasurementsDoNotImplyOffline() {
        var statuses = StationStatusCalculator.systemStatuses(
                Map.of(), Map.of("life-support", "ONLINE", "communications", "ONLINE"), NOW);
        assertThat(statuses.get("life-support")).isEqualTo(MetricStatus.UNKNOWN);
        assertThat(statuses.get("communications")).isEqualTo(MetricStatus.NOMINAL);
        assertThat(StationStatusCalculator.statusOf(oxygen(21, NOW.minusSeconds(31)), NOW))
                .isEqualTo(MetricStatus.UNKNOWN);
    }

    @Test
    void explicitOfflineDoesNotHideMoreSevereThresholdStates() {
        var statuses = StationStatusCalculator.systemStatuses(
                Map.of("oxygen", oxygen(16, NOW)), Map.of("life-support", "OFFLINE"), NOW);
        assertThat(statuses.get("life-support")).isEqualTo(MetricStatus.CRITICAL);
    }

    @Test
    void wrongUnitsAndInvalidValuesAreUnknown() {
        var wrongUnit = new MeasurementSnapshot(
                "oxygen", "life-support", "oxygen-level", "Oxygen", 21, "ppm", NOW.toString());
        assertThat(StationStatusCalculator.statusOf(wrongUnit, NOW)).isEqualTo(MetricStatus.UNKNOWN);
        assertThat(StationStatusCalculator.isPhysicallyValid(wrongUnit)).isFalse();
        assertThat(StationStatusCalculator.statusOf(oxygen(-1, NOW), NOW)).isEqualTo(MetricStatus.UNKNOWN);
    }

    private MeasurementSnapshot oxygen(double value, Instant measuredAt) {
        return new MeasurementSnapshot(
                "oxygen", "life-support", "oxygen-level", "Oxygen", value, "% vol.", measuredAt.toString());
    }
}
