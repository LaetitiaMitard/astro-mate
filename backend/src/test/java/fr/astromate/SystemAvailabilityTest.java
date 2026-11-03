package fr.astromate;

import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite::memory:",
                "astromate.simulation.enabled=false",
                "spring.application.name=availability-tests"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SystemAvailabilityTest {
    @Autowired
    private MeasurementService measurements;

    @Autowired
    private ThermalSimulation simulation;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HikariDataSource dataSource;

    @MockitoBean
    private Clock clock;

    @Test
    void journalsOnlyActualAvailabilityTransitionsWithTheirSeverity() {
        measurements.setSystemAvailability("thermal-control", "OFFLINE");
        measurements.setSystemAvailability("thermal-control", "OFFLINE");
        JsonNode offline = get("/api/v1/systems/thermal-control");
        assertThat(offline.path("availability").asText()).isEqualTo("OFFLINE");
        assertThat(offline.path("status").asText()).isEqualTo("OFFLINE");

        measurements.setSystemAvailability("thermal-control", "ONLINE");
        measurements.setSystemAvailability("thermal-control", "ONLINE");
        JsonNode events = get("/api/v1/events?category=SYSTEM&sort=occurredAt,asc")
                .path("items");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).path("level").asText()).isEqualTo("WARNING");
        assertThat(events.get(0).path("systemId").asText()).isEqualTo("thermal-control");
        assertThat(events.get(0).path("message").asText()).contains("hors ligne");
        assertThat(events.get(1).path("level").asText()).isEqualTo("INFO");
        assertThat(events.get(1).path("message").asText()).contains("en ligne");
        assertThat(get("/api/v1/alerts").path("items")).isEmpty();
    }

    @Test
    void suspendsOfflineTelemetryWithoutStalenessEventsAndResumesOnReturn() {
        Instant startedAt = Instant.now();
        when(clock.instant()).thenReturn(startedAt);
        simulation.produceTelemetry();
        JsonNode before = get("/api/v1/systems/thermal-control");
        measurements.setSystemAvailability("thermal-control", "OFFLINE");

        try {
            when(clock.instant()).thenReturn(startedAt.plusSeconds(40));
            simulation.produceTelemetry();
            measurements.recordMeasurement("radiatorATemperature", 31, startedAt.plusSeconds(41));
            JsonNode offline = get("/api/v1/systems/thermal-control");
            assertThat(offline.path("metrics")).isEqualTo(before.path("metrics"));
            assertThat(offline.path("updatedAt")).isEqualTo(before.path("updatedAt"));
            assertThat(offline.path("status").asText()).isEqualTo("OFFLINE");
            JsonNode dashboard = get("/api/v1/dashboard");
            assertThat(dashboard.path("indicators").get(0).path("measuredAt").asText())
                    .isEqualTo(startedAt.plusSeconds(40).toString());

            measurements.recordStaleMeasurements(startedAt.plusSeconds(41));
            measurements.recordStaleMeasurements(startedAt.plusSeconds(42));
            for (JsonNode event : get("/api/v1/systems/thermal-control").path("recentEvents")) {
                assertThat(event.path("category").asText()).isNotEqualTo("DATA");
            }
            assertThat(get("/api/v1/alerts").path("items")).isEmpty();
        } finally {
            measurements.setSystemAvailability("thermal-control", "ONLINE");
        }

        when(clock.instant()).thenReturn(startedAt.plusSeconds(50));
        simulation.produceTelemetry();
        JsonNode recovered = get("/api/v1/systems/thermal-control");
        assertThat(recovered.path("availability").asText()).isEqualTo("ONLINE");
        for (JsonNode metric : recovered.path("metrics")) {
            assertThat(metric.path("measuredAt").asText())
                    .isEqualTo(startedAt.plusSeconds(50).toString());
        }
    }

    @Test
    void keepsOfflineDistinctFromUnknownWhenItsLastMeasurementsAge() {
        measurements.setSystemAvailability("thermal-control", "OFFLINE");
        Instant measuredAt = Instant.now().minusSeconds(31);
        jdbcTemplate.update(
                "UPDATE metric_measurement SET measured_at = ? WHERE system_id = 'thermal-control'",
                measuredAt.toString()
        );
        measurements.recordStaleMeasurements(Instant.now());

        JsonNode detail = get("/api/v1/systems/thermal-control");
        assertThat(detail.path("status").asText()).isEqualTo("OFFLINE");
        for (JsonNode metric : detail.path("metrics")) {
            assertThat(metric.path("status").asText()).isEqualTo("UNKNOWN");
            assertThat(metric.path("measuredAt").asText()).isEqualTo(measuredAt.toString());
        }
        for (JsonNode event : detail.path("recentEvents")) {
            assertThat(event.path("category").asText()).isNotEqualTo("DATA");
        }

        measurements.setSystemAvailability("thermal-control", "ONLINE");
        assertThat(get("/api/v1/systems/thermal-control").path("status").asText())
                .isEqualTo("UNKNOWN");
        assertThat(get("/api/v1/alerts").path("items")).isEmpty();
    }

    @Test
    void returnsServiceUnavailableWithoutValuesWhenSqliteCannotBeAccessed() {
        dataSource.close();

        for (String path : new String[] {
                "/api/v1/dashboard",
                "/api/v1/systems/thermal-control",
                "/api/v1/systems/thermal-control/indicators/temperature/measurements",
                "/api/v1/events",
                "/api/v1/alerts"
        }) {
            var response = restTemplate.getForEntity(path, JsonNode.class);
            assertThat(response.getStatusCode().value()).isEqualTo(503);
            assertThat(response.getHeaders().getContentType().toString())
                    .startsWith("application/problem+json");
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().path("title").asText())
                    .isEqualTo("Service indisponible");
            assertThat(response.getBody().has("indicators")).isFalse();
        }
    }

    private JsonNode get(String path) {
        var response = restTemplate.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }
}
