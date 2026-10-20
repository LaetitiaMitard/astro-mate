package fr.astromate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:sqlite::memory:"
)
class AstroMateApplicationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Test
    void startsWithLocalSqliteAndServesTheBuiltFrontend() {
        assertThat(environment.getProperty("server.address"))
                .isEqualTo("127.0.0.1");
        assertThat(jdbcTemplate.queryForObject("SELECT sqlite_version()", String.class))
                .isNotBlank();

        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("<title>AstroMate</title>");
    }

    @Test
    void initializesAndServesTheNominalDashboard() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/dashboard",
                JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode dashboard = response.getBody();
        assertThat(dashboard).isNotNull();
        assertThat(dashboard.path("stationStatus").asText()).isEqualTo("NOMINAL");
        assertThat(dashboard.path("simulationStatus").asText()).isEqualTo("ACTIVE");
        assertThat(dashboard.path("recentOpenAlerts")).isEmpty();

        JsonNode indicators = dashboard.path("indicators");
        assertThat(indicators).hasSize(4);
        assertIndicator(indicators, "oxygen", "21", "% vol.", "19", "23");
        assertIndicator(indicators, "carbonDioxide", "400", "ppm", "0", "1000");
        assertIndicator(indicators, "temperature", "22.3", "°C", "18", "26");
        assertIndicator(indicators, "availableEnergy", "78", "%", "20", "100");
    }

    private void assertIndicator(
            JsonNode indicators,
            String key,
            String value,
            String unit,
            String nominalMin,
            String nominalMax
    ) {
        JsonNode indicator = null;
        for (JsonNode candidate : indicators) {
            if (candidate.path("key").asText().equals(key)) {
                indicator = candidate;
                break;
            }
        }

        assertThat(indicator).as("indicator %s", key).isNotNull();
        assertThat(indicator.path("value").decimalValue())
                .isEqualByComparingTo(value);
        assertThat(indicator.path("unit").asText()).isEqualTo(unit);
        assertThat(indicator.path("status").asText()).isEqualTo("NOMINAL");
        assertThat(indicator.path("nominalRange").path("min").decimalValue())
                .isEqualByComparingTo(nominalMin);
        assertThat(indicator.path("nominalRange").path("max").decimalValue())
                .isEqualByComparingTo(nominalMax);
    }
}
