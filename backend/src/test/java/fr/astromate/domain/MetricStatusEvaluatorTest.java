package fr.astromate.domain;

import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.service.MetricStatusEvaluator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetricStatusEvaluatorTest {
    @ParameterizedTest
    @CsvSource({
            "oxygen, 19, NOMINAL",
            "oxygen, 23, NOMINAL",
            "oxygen, 17, WARNING",
            "oxygen, 16.9, CRITICAL",
            "oxygen, 25, WARNING",
            "oxygen, 25.1, CRITICAL",
            "oxygen, -1, UNKNOWN",
            "carbonDioxide, 1000, NOMINAL",
            "carbonDioxide, 2000, WARNING",
            "carbonDioxide, 2001, CRITICAL",
            "radiatorATemperature, 15, WARNING",
            "radiatorATemperature, 18, NOMINAL",
            "radiatorATemperature, 26, NOMINAL",
            "radiatorATemperature, 30, WARNING",
            "radiatorATemperature, 31, CRITICAL",
            "radiatorATemperature, -274, UNKNOWN",
            "availableEnergy, 10, WARNING",
            "availableEnergy, 9.9, CRITICAL",
            "availableEnergy, 101, UNKNOWN",
            "thermalConsumption, 1.99, NOMINAL",
            "thermalConsumption, 2, WARNING"
    })
    void evaluatesBoundariesWithoutSpring(String key, double value, MetricStatus expected) {
        assertThat(MetricStatusEvaluator.evaluate(key, value)).isEqualTo(expected);
    }

    @Test
    void rejectsNonFiniteAndMissingMeasurements() {
        for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThat(MetricStatusEvaluator.isPhysicallyValid("oxygen", value)).isFalse();
            assertThat(MetricStatusEvaluator.evaluate("oxygen", value)).isEqualTo(MetricStatus.UNKNOWN);
        }
        assertThat(MetricStatusEvaluator.evaluate("oxygen", null)).isEqualTo(MetricStatus.UNKNOWN);
        assertThatThrownBy(() -> MetricStatusEvaluator.ruleFor("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void becomesStaleOnlyAfterThirtySeconds() {
        Instant measuredAt = Instant.parse("2026-11-04T12:00:00Z");
        assertThat(MetricStatusEvaluator.isStale(measuredAt, measuredAt.plusSeconds(30))).isFalse();
        assertThat(MetricStatusEvaluator.isStale(measuredAt, measuredAt.plusSeconds(30).plusNanos(1)))
                .isTrue();
    }

    @Test
    void preservesSeverityOrderIncludingOfflineAndUnknown() {
        assertThat(MetricStatusEvaluator.mostSevere(List.of())).isEqualTo(MetricStatus.NOMINAL);
        assertThat(MetricStatusEvaluator.mostSevere(List.of(MetricStatus.UNKNOWN, MetricStatus.OFFLINE)))
                .isEqualTo(MetricStatus.OFFLINE);
        assertThat(MetricStatusEvaluator.mostSevere(List.of(MetricStatus.OFFLINE, MetricStatus.WARNING)))
                .isEqualTo(MetricStatus.WARNING);
        assertThat(MetricStatusEvaluator.mostSevere(List.of(MetricStatus.WARNING, MetricStatus.CRITICAL)))
                .isEqualTo(MetricStatus.CRITICAL);
    }
}
