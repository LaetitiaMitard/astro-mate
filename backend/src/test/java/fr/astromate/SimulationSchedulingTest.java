package fr.astromate;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:sqlite::memory:"
)
class SimulationSchedulingTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void automaticallyProducesTelemetryEveryTenSeconds() {
        Instant firstMeasurement = lastMeasurementAt();

        await().atMost(Duration.ofSeconds(12)).untilAsserted(() -> {
            Instant nextMeasurement = lastMeasurementAt();
            assertThat(nextMeasurement).isAfter(firstMeasurement);
            assertThat(Duration.between(firstMeasurement, nextMeasurement).toMillis())
                    .isBetween(9_000L, 11_000L);
        });
    }

    private Instant lastMeasurementAt() {
        var response = restTemplate.getForEntity("/api/v1/dashboard", JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        return Instant.parse(response.getBody().path("lastMeasurementAt").asText());
    }
}
