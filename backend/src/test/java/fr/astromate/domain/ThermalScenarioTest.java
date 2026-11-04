package fr.astromate.domain;

import fr.astromate.domain.service.ThermalScenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ThermalScenarioTest {
    @ParameterizedTest
    @CsvSource({
            "0, 22.1, 22.4",
            "59999, 22.1, 22.4",
            "60000, 27, 27",
            "119999, 27, 27",
            "120000, 31, 31",
            "179999, 31, 31",
            "180000, 22.1, 22.4",
            "239999, 22.1, 22.4"
    })
    void selectsThePhaseAtExactBoundaries(long elapsedMillis, double radiatorA, double radiatorB) {
        assertThat(ThermalScenario.temperatures(elapsedMillis))
                .isEqualTo(new ThermalScenario.Temperatures(radiatorA, radiatorB));
    }
}
