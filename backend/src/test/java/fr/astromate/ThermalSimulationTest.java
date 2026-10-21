package fr.astromate;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite::memory:",
                "astromate.simulation.enabled=false"
        }
)
class ThermalSimulationTest {
    @Autowired
    private ThermalSimulation simulation;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private Clock clock;

    @Test
    void repeatsFourSixtySecondPhasesWithFreshNominalSupportingMetrics() {
        Instant startedAt = Instant.now();
        double[][] temperatures = {
                {22.1, 22.4}, {27, 27}, {31, 31}, {22.1, 22.4},
                {22.1, 22.4}, {27, 27}, {31, 31}, {22.1, 22.4}
        };
        String[] statuses = {
                "NOMINAL", "WARNING", "CRITICAL", "NOMINAL",
                "NOMINAL", "WARNING", "CRITICAL", "NOMINAL"
        };

        for (int sample = 0; sample < 48; sample++) {
            Instant measuredAt = startedAt.plusSeconds(sample * 10L);
            when(clock.instant()).thenReturn(measuredAt);
            simulation.produceTelemetry();

            int phase = sample / 6;
            JsonNode detail = get("/api/v1/systems/thermal-control");
            assertThat(detail.path("status").asText()).isEqualTo(statuses[phase]);
            assertThat(find(detail.path("components"), "id", "radiator-a")
                    .path("value").asDouble()).isEqualTo(temperatures[phase][0]);
            assertThat(find(detail.path("components"), "id", "radiator-b")
                    .path("value").asDouble()).isEqualTo(temperatures[phase][1]);
            for (JsonNode metric : detail.path("metrics")) {
                assertThat(Instant.parse(metric.path("measuredAt").asText()))
                        .isEqualTo(measuredAt);
                if (!metric.path("id").asText().startsWith("radiator-")) {
                    assertThat(metric.path("status").asText()).isEqualTo("NOMINAL");
                }
            }

            JsonNode dashboard = get("/api/v1/dashboard");
            assertThat(dashboard.path("stationStatus").asText())
                    .isEqualTo(statuses[phase]);
            assertThat(find(dashboard.path("indicators"), "key", "temperature")
                    .path("status").asText()).isEqualTo(statuses[phase]);
            for (JsonNode indicator : dashboard.path("indicators")) {
                assertThat(Instant.parse(indicator.path("measuredAt").asText()))
                        .isEqualTo(measuredAt);
                if (!indicator.path("key").asText().equals("temperature")) {
                    assertThat(indicator.path("status").asText()).isEqualTo("NOMINAL");
                }
            }
        }
    }

    private JsonNode get(String path) {
        var response = restTemplate.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private JsonNode find(JsonNode items, String key, String value) {
        for (JsonNode item : items) {
            if (item.path(key).asText().equals(value)) {
                return item;
            }
        }
        throw new AssertionError("Missing item: " + value);
    }
}
