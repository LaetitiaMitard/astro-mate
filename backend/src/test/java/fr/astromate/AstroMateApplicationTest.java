package fr.astromate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private MeasurementService measurementService;

    @BeforeEach
    void resetDashboardMeasurements() {
        jdbcTemplate.update(
                """
                INSERT OR IGNORE INTO metric_measurement
                    (metric_key, system_id, metric_id, name, value, unit, measured_at)
                VALUES ('oxygen', 'life-support', 'oxygen-level', 'O₂', 21, '% vol.', ?)
                """,
                Instant.now().toString()
        );
        recordMeasurement("oxygen", 21);
        recordMeasurement("carbonDioxide", 400);
        recordMeasurement("radiatorATemperature", 22.1);
        recordMeasurement("radiatorBTemperature", 22.4);
        recordMeasurement("availableEnergy", 78);
        recordMeasurement("fluidFlow", 120);
        recordMeasurement("pressure", 2.1);
        recordMeasurement("thermalConsumption", 1.2);
        for (String systemId : new String[] {
                "life-support", "thermal-control", "power-supply", "communications"
        }) {
            measurementService.setSystemAvailability(systemId, "ONLINE");
        }
    }

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

    @ParameterizedTest
    @CsvSource({
            "19, NOMINAL",
            "23, NOMINAL",
            "17, WARNING",
            "25, WARNING",
            "16.9, CRITICAL",
            "25.1, CRITICAL"
    })
    void evaluatesOxygenAtInclusiveAndStrictCriticalBoundaries(
            double value,
            String expectedStatus
    ) {
        recordMeasurement("oxygen", value);

        JsonNode indicator = getDashboardIndicator("oxygen");

        assertThat(indicator.path("status").asText()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "0, NOMINAL",
            "1000, NOMINAL",
            "1000.1, WARNING",
            "2000, WARNING",
            "2000.1, CRITICAL"
    })
    void evaluatesCarbonDioxideAtNominalAndCriticalBoundaries(
            double value,
            String expectedStatus
    ) {
        recordMeasurement("carbonDioxide", value);

        JsonNode indicator = getDashboardIndicator("carbonDioxide");

        assertThat(indicator.path("status").asText()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "radiatorATemperature, 18, NOMINAL",
            "radiatorATemperature, 26, NOMINAL",
            "radiatorATemperature, 17.9, WARNING",
            "radiatorATemperature, 26.1, WARNING",
            "availableEnergy, 20, NOMINAL",
            "availableEnergy, 100, NOMINAL",
            "availableEnergy, 19.9, WARNING",
            "availableEnergy, 10, WARNING",
            "availableEnergy, 9.9, CRITICAL"
    })
    void evaluatesTemperatureAndEnergyBoundaries(
            String metricKey,
            double value,
            String expectedStatus
    ) {
        recordMeasurement(metricKey, value);

        String indicatorKey = metricKey.equals("radiatorATemperature")
                ? "temperature"
                : metricKey;
        assertThat(getDashboardIndicator(indicatorKey).path("status").asText())
                .isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "oxygen, 17, oxygen, WARNING",
            "oxygen, 16.9, oxygen, CRITICAL",
            "oxygen, 25, oxygen, WARNING",
            "oxygen, 25.1, oxygen, CRITICAL",
            "carbonDioxide, 2000, carbonDioxide, WARNING",
            "carbonDioxide, 2000.1, carbonDioxide, CRITICAL",
            "radiatorATemperature, 15, temperature, WARNING",
            "radiatorATemperature, 14.9, temperature, CRITICAL",
            "radiatorATemperature, 30, temperature, WARNING",
            "radiatorATemperature, 30.1, temperature, CRITICAL"
    })
    void criticalStatusBeginsStrictlyBeyondEachConfiguredThreshold(
            String metricKey,
            double value,
            String indicatorKey,
            String expectedStatus
    ) {
        recordMeasurement(metricKey, value);

        assertThat(getDashboardIndicator(indicatorKey).path("status").asText())
                .isEqualTo(expectedStatus);
    }

    @Test
    void displaysRoundedRadiatorAverageAndUsesTheMostSevereSourceStatus() {
        JsonNode nominalTemperature = getDashboardIndicator("temperature");
        assertThat(nominalTemperature.path("value").decimalValue())
                .isEqualByComparingTo("22.3");
        assertThat(nominalTemperature.path("status").asText()).isEqualTo("NOMINAL");

        recordMeasurement("radiatorATemperature", 31);
        JsonNode criticalTemperature = getDashboardIndicator("temperature");

        assertThat(criticalTemperature.path("value").decimalValue())
                .isEqualByComparingTo("26.7");
        assertThat(criticalTemperature.path("status").asText()).isEqualTo("CRITICAL");
    }

    @Test
    void servesThermalMetricsAndComponentStatesFromTheirCurrentMeasurements() {
        recordMeasurement("radiatorATemperature", 31);

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
        "/api/v1/systems/thermal-control",
        JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode detail = response.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.path("status").asText()).isEqualTo("CRITICAL");
        JsonNode radiatorA = findById(detail.path("components"), "radiator-a");
        assertThat(radiatorA.path("status").asText()).isEqualTo("CRITICAL");
        assertThat(radiatorA.path("value").asDouble()).isEqualTo(31);
        assertThat(radiatorA.path("simulated").asBoolean()).isFalse();

        JsonNode pump = findById(detail.path("components"), "main-pump");
        assertThat(pump.path("status").asText()).isEqualTo("NOMINAL");
        assertThat(pump.path("value").isNull()).isTrue();
        assertThat(pump.path("simulated").asBoolean()).isTrue();
        assertThat(pump.path("updatedAt").isNull()).isFalse();

        JsonNode radiatorMetric = findById(
        detail.path("metrics"),
        "radiator-a-temperature"
        );
        JsonNode dashboardTemperatureIndicator = findByKey(
        detail.path("indicators"),
        "temperature"
        );
        assertThat(dashboardTemperatureIndicator.path("value").asDouble())
        .isEqualTo(26.7);
        assertThat(dashboardTemperatureIndicator.path("status").asText())
        .isEqualTo("CRITICAL");
        assertThat(radiatorMetric.path("status").asText()).isEqualTo("CRITICAL");
        assertThat(radiatorMetric.path("criticalLimits")
        .path("maxExclusive").asDouble()).isEqualTo(30);
        assertThat(radiatorMetric.path("nominalRange")
        .path("max").asDouble()).isEqualTo(26);
        assertThat(detail.path("metrics")).hasSize(5);
        assertThat(detail.path("components")).hasSize(4);
    }

    @ParameterizedTest
    @CsvSource({
            "1.999, NOMINAL",
            "2.0, WARNING"
    })
    void treatsTheThermalConsumptionUpperBoundAsExclusive(
            double value,
            String expectedStatus
    ) {
        recordMeasurement("thermalConsumption", value);

        JsonNode dashboard = getDashboard();

        assertThat(dashboard.path("stationStatus").asText()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "fluidFlow, 80, NOMINAL",
            "fluidFlow, 200, NOMINAL",
            "fluidFlow, 79.9, WARNING",
            "fluidFlow, 200.1, WARNING",
            "pressure, 1.5, NOMINAL",
            "pressure, 3.0, NOMINAL",
            "pressure, 1.4, WARNING",
            "pressure, 3.1, WARNING"
    })
    void evaluatesInclusiveBoundsForThermalFlowAndPressure(
            String metricKey,
            double value,
            String expectedStatus
    ) {
        recordMeasurement(metricKey, value);

        assertThat(getDashboard().path("stationStatus").asText()).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "oxygen, 101",
            "carbonDioxide, -0.1",
            "radiatorATemperature, -273.16",
            "availableEnergy, 100.1",
            "fluidFlow, -0.1",
            "pressure, -0.1",
            "thermalConsumption, -0.1"
    })
    void rejectsMeasurementsOutsideTheirPhysicalDomain(
            String metricKey,
            double value
    ) {
        assertThatThrownBy(() -> recordMeasurement(metricKey, value))
                .isInstanceOf(IllegalArgumentException.class);
        if (metricKey.equals("oxygen")) {
            assertThat(getDashboardIndicator("oxygen").path("value").asDouble())
                    .isEqualTo(21);
        }
    }

    @Test
    void aggregatesStationStatusesInSeverityOrderWithoutHidingKnownAnomalies() {
        measurementService.setSystemAvailability("thermal-control", "OFFLINE");
        recordMeasurement("availableEnergy", 5);
        JsonNode criticalDashboard = getDashboard();
        assertThat(criticalDashboard.path("stationStatus").asText()).isEqualTo("CRITICAL");
        assertThat(getSystemStatus(criticalDashboard, "power-supply")).isEqualTo("CRITICAL");
        assertThat(getSystemStatus(criticalDashboard, "thermal-control")).isEqualTo("OFFLINE");

        recordMeasurement("availableEnergy", 78);
        recordMeasurement("oxygen", 18);
        JsonNode warningDashboard = getDashboard();
        assertThat(warningDashboard.path("stationStatus").asText()).isEqualTo("WARNING");
        assertThat(getSystemStatus(warningDashboard, "life-support")).isEqualTo("WARNING");

        recordMeasurement("oxygen", 21);
        jdbcTemplate.update(
                "DELETE FROM metric_measurement WHERE metric_key = 'oxygen'"
        );
        JsonNode offlineDashboard = getDashboard();
        assertThat(offlineDashboard.path("stationStatus").asText()).isEqualTo("OFFLINE");
        assertThat(getSystemStatus(offlineDashboard, "life-support")).isEqualTo("UNKNOWN");
        assertThat(getSystemStatus(offlineDashboard, "thermal-control")).isEqualTo("OFFLINE");
        assertThat(offlineDashboard.path("offlineSystems").toString())
                .contains("thermal-control");

        measurementService.setSystemAvailability("thermal-control", "ONLINE");
        JsonNode unknownDashboard = getDashboard();
        assertThat(unknownDashboard.path("stationStatus").asText()).isEqualTo("UNKNOWN");
        assertThat(getSystemStatus(unknownDashboard, "life-support")).isEqualTo("UNKNOWN");

        recordMeasurement("carbonDioxide", 1001);
        assertThat(getDashboard().path("stationStatus").asText()).isEqualTo("WARNING");
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

    private JsonNode getDashboardIndicator(String key) {
        JsonNode dashboard = getDashboard();
        for (JsonNode indicator : dashboard.path("indicators")) {
            if (indicator.path("key").asText().equals(key)) {
                return indicator;
            }
        }
        throw new AssertionError("Missing dashboard indicator: " + key);
    }

    private JsonNode findById(JsonNode items, String id) {
        for (JsonNode item : items) {
            if (item.path("id").asText().equals(id)) {
                return item;
            }
        }
        throw new AssertionError("Missing item: " + id);
    }

    private JsonNode findByKey(JsonNode items, String key) {
        for (JsonNode item : items) {
            if (item.path("key").asText().equals(key)) {
                return item;
            }
        }
        throw new AssertionError("Missing item: " + key);
    }

    private JsonNode getDashboard() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/dashboard",
                JsonNode.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode dashboard = response.getBody();
        assertThat(dashboard).isNotNull();
        return dashboard;
    }

    private String getSystemStatus(JsonNode dashboard, String systemId) {
        for (JsonNode system : dashboard.path("systemStatuses")) {
            if (system.path("systemId").asText().equals(systemId)) {
                return system.path("status").asText();
            }
        }
        throw new AssertionError("Missing system status: " + systemId);
    }

    private void recordMeasurement(String metricKey, double value) {
        measurementService.recordMeasurement(metricKey, value, Instant.now());
    }
}
