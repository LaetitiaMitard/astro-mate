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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite::memory:",
                "astromate.simulation.enabled=false"
        }
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

    @Autowired
    private MeasurementChangeNotifier measurementChangeNotifier;

    @BeforeEach
    void resetDashboardMeasurements() {
        jdbcTemplate.update("DELETE FROM measurement_history");
        jdbcTemplate.update(
                "UPDATE metric_measurement SET measured_at = '1970-01-01T00:00:00Z'"
        );
        jdbcTemplate.update(
                "UPDATE dashboard_indicator SET measured_at = '1970-01-01T00:00:00Z'"
        );
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
        jdbcTemplate.update("DELETE FROM station_event");
        jdbcTemplate.update("DELETE FROM alert_episode");
        jdbcTemplate.update("DELETE FROM measurement_history");
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

    @Test
    void servesRecentEventsInPagesOfTwentyNewestFirst() {
        Instant now = Instant.now();
        for (int index = 0; index < 22; index++) {
            jdbcTemplate.update(
                    """
                    INSERT INTO station_event
                        (id, system_id, level, category, source, message, occurred_at)
                    VALUES (?, 'thermal-control', 'INFO', 'OPERATION', 'test', ?, ?)
                    """,
                    "event-" + index,
                    "Événement " + index,
                    now.minusSeconds(index).toString()
            );
        }
        jdbcTemplate.update(
                """
                INSERT INTO station_event
                    (id, system_id, level, category, source, message, occurred_at)
                VALUES ('old-event', NULL, 'INFO', 'DATA', 'test', 'Ancien événement', ?)
                """,
                now.minus(Duration.ofDays(8)).toString()
        );

        ResponseEntity<JsonNode> firstPage = restTemplate.getForEntity(
                "/api/v1/events",
                JsonNode.class
        );
        ResponseEntity<JsonNode> secondPage = restTemplate.getForEntity(
                "/api/v1/events?page=1",
                JsonNode.class
        );
        ResponseEntity<JsonNode> oldestFirst = restTemplate.getForEntity(
                "/api/v1/events?pageSize=2&sort=occurredAt,asc",
                JsonNode.class
        );
        ResponseEntity<JsonNode> filtered = restTemplate.getForEntity(
                "/api/v1/events?level=INFO&category=OPERATION&q=Événement 21",
                JsonNode.class
        );

        assertThat(firstPage.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode firstPageBody = firstPage.getBody();
        assertThat(firstPageBody).isNotNull();
        assertThat(firstPageBody.path("items")).hasSize(20);
        assertThat(firstPageBody.path("items").get(0).path("id").asText())
                .isEqualTo("event-0");
        assertThat(firstPageBody.path("items").get(19).path("id").asText())
                .isEqualTo("event-19");
        assertThat(firstPageBody.path("page").asInt()).isZero();
        assertThat(firstPageBody.path("pageSize").asInt()).isEqualTo(20);
        assertThat(firstPageBody.path("totalElements").asInt()).isEqualTo(22);
        assertThat(firstPageBody.path("totalPages").asInt()).isEqualTo(2);

        JsonNode secondPageBody = secondPage.getBody();
        assertThat(secondPageBody).isNotNull();
        assertThat(secondPageBody.path("items")).hasSize(2);
        assertThat(secondPageBody.path("items").get(0).path("id").asText())
                .isEqualTo("event-20");
        assertThat(secondPageBody.path("page").asInt()).isEqualTo(1);
        assertThat(secondPageBody.path("totalElements").asInt()).isEqualTo(22);

        JsonNode oldestFirstBody = oldestFirst.getBody();
        assertThat(oldestFirst.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(oldestFirstBody).isNotNull();
        assertThat(oldestFirstBody.path("items")).hasSize(2);
        assertThat(oldestFirstBody.path("items").get(0).path("id").asText())
                .isEqualTo("event-21");
        assertThat(oldestFirstBody.path("items").get(1).path("id").asText())
                .isEqualTo("event-20");
        assertThat(oldestFirstBody.path("pageSize").asInt()).isEqualTo(2);

        JsonNode filteredBody = filtered.getBody();
        assertThat(filtered.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(filteredBody).isNotNull();
        assertThat(filteredBody.path("items")).hasSize(1);
        assertThat(filteredBody.path("items").get(0).path("id").asText())
                .isEqualTo("event-21");
        assertThat(filteredBody.path("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void publishesMeasurementChangesAfterTheDatabaseTransactionCommits() throws Exception {
        SseEmitter subscriber = mock(SseEmitter.class);
        measurementChangeNotifier.subscribe(subscriber);

        measurementService.recordMeasurement("oxygen", 20, Instant.now());

        verify(subscriber, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void servesTemperatureHistoryAsTheRoundedAverageOfRadiators() {
        Instant sampledAt = Instant.now();
        measurementService.recordMeasurement(
                "radiatorATemperature",
                30,
                sampledAt
        );
        measurementService.recordMeasurement(
                "radiatorBTemperature",
                27.1,
                sampledAt
        );

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/systems/thermal-control/indicators/temperature/measurements",
                JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode series = response.getBody();
        assertThat(series).isNotNull();
        assertThat(series.path("indicatorKey").asText()).isEqualTo("temperature");
        assertThat(series.path("unit").asText()).isEqualTo("°C");
        JsonNode sourceMetricIds = series.path("sourceMetricIds");
        assertThat(sourceMetricIds).hasSize(2);
        assertThat(sourceMetricIds.get(0).asText())
                .isEqualTo("radiator-a-temperature");
        assertThat(sourceMetricIds.get(1).asText())
                .isEqualTo("radiator-b-temperature");
        JsonNode measurements = series.path("measurements");
        assertThat(measurements).hasSize(1);
        assertThat(measurements.get(0).path("value").decimalValue())
                .isEqualByComparingTo("28.6");
        assertThat(measurements.get(0).path("measuredAt").asText())
                .isEqualTo(sampledAt.toString());
    }

    @Test
    void limitsTemperatureHistoryToTheRequestedSixHourWindow() {
        Instant now = Instant.now();
        Instant outsideWindow = now.minus(Duration.ofHours(6)).minusSeconds(5);
        Instant withinWindow = now.minus(Duration.ofHours(2));
        Instant future = now.plusSeconds(30);
        insertTemperaturePair(outsideWindow, 20, 20);
        insertTemperaturePair(withinWindow, 21.4, 22.1);
        insertTemperaturePair(future, 24, 24);

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/systems/thermal-control/indicators/temperature/measurements",
                JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode measurements = response.getBody().path("measurements");
        assertThat(measurements).hasSize(1);
        assertThat(measurements.get(0).path("measuredAt").asText())
                .isEqualTo(withinWindow.toString());
        assertThat(measurements.get(0).path("value").decimalValue())
                .isEqualByComparingTo("21.8");
    }

    @Test
    void includesTheRequestedStartAndExcludesTheRequestedEnd() {
        Instant from = Instant.now().minus(Duration.ofHours(3));
        Instant inside = from.plusSeconds(60);
        Instant to = inside.plusSeconds(60);
        insertTemperaturePair(from, 20, 22);
        insertTemperaturePair(inside, 24, 26);
        insertTemperaturePair(to, 28, 30);

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/systems/thermal-control/indicators/temperature/measurements"
                        + "?from={from}&to={to}",
                JsonNode.class,
                from.toString(),
                to.toString()
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode measurements = response.getBody().path("measurements");
        assertThat(measurements).hasSize(2);
        assertThat(measurements.get(0).path("measuredAt").asText())
                .isEqualTo(from.toString());
        assertThat(measurements.get(1).path("measuredAt").asText())
                .isEqualTo(inside.toString());
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
    void followsThermalAlertEpisodesAndLogsOnlyMeaningfulTransitions() {
        recordMeasurement("radiatorATemperature", 27);

        JsonNode warningDashboard = getDashboard();
        assertThat(warningDashboard.path("recentOpenAlerts")).hasSize(1);
        assertThat(warningDashboard.path("recentOpenAlerts").get(0).path("level").asText())
                .isEqualTo("WARNING");
        assertThat(warningDashboard.path("recentOpenAlerts").get(0).path("metricId").asText())
                .isEqualTo("radiator-a-temperature");
        JsonNode warningEvents = getThermalEvents();
        assertThat(warningEvents).hasSize(2);
        assertThat(warningEvents.findValuesAsText("level"))
                .containsExactlyInAnyOrder("WARNING", "WARNING");
        assertThat(warningEvents.get(0).path("category").asText()).isEqualTo("ALERT");

        recordMeasurement("radiatorATemperature", 27);
        assertThat(getThermalEvents()).hasSize(2);

        recordMeasurement("radiatorATemperature", 31);
        JsonNode criticalDashboard = getDashboard();
        assertThat(criticalDashboard.path("recentOpenAlerts")).hasSize(1);
        assertThat(criticalDashboard.path("recentOpenAlerts").get(0).path("level").asText())
                .isEqualTo("CRITICAL");
        JsonNode criticalEvents = getThermalEvents();
        assertThat(criticalEvents).hasSize(5);
        assertThat(criticalEvents.findValuesAsText("level"))
                .containsExactlyInAnyOrder(
                        "WARNING", "WARNING", "CRITICAL", "INFO", "CRITICAL"
                );

        recordMeasurement("radiatorATemperature", 22.1);
        assertThat(getDashboard().path("recentOpenAlerts")).isEmpty();
        JsonNode resolvedEvents = getThermalEvents();
        assertThat(resolvedEvents).hasSize(7);
        assertThat(resolvedEvents.findValuesAsText("level"))
                .containsExactlyInAnyOrder(
                        "WARNING", "WARNING", "CRITICAL", "INFO", "CRITICAL", "INFO", "INFO"
                );

        recordMeasurement("radiatorATemperature", 22.1);
        assertThat(getThermalEvents()).hasSize(7);
    }

    @Test
    void deescalatesCriticalThermalAlertIntoANewWarningEpisode() {
        recordMeasurement("radiatorATemperature", 31);
        assertThat(getDashboard().path("recentOpenAlerts").get(0).path("level").asText())
                .isEqualTo("CRITICAL");

        recordMeasurement("radiatorATemperature", 27);
        JsonNode warningDashboard = getDashboard();
        assertThat(warningDashboard.path("recentOpenAlerts")).hasSize(1);
        assertThat(warningDashboard.path("recentOpenAlerts").get(0).path("level").asText())
                .isEqualTo("WARNING");
        assertThat(getThermalEvents().findValuesAsText("level"))
                .containsExactlyInAnyOrder(
                        "CRITICAL", "CRITICAL", "WARNING", "INFO", "WARNING"
                );
    }

    @Test
    void ignoresOutOfOrderMeasurementsWithoutClosingAnOpenAlert() {
        Instant newerMeasurement = Instant.now().plusSeconds(5);
        measurementService.recordMeasurement(
                "radiatorATemperature",
                31,
                newerMeasurement
        );

        assertThatThrownBy(() -> measurementService.recordMeasurement(
                "radiatorATemperature",
                22.1,
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(getDashboard().path("recentOpenAlerts").get(0).path("level").asText())
                .isEqualTo("CRITICAL");
        assertThat(getThermalEvents()).hasSize(2);
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

    private JsonNode getThermalEvents() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/v1/systems/thermal-control",
                JsonNode.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode detail = response.getBody();
        assertThat(detail).isNotNull();
        return detail.path("recentEvents");
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

    private void insertTemperaturePair(Instant measuredAt, double radiatorA, double radiatorB) {
        insertHistory("radiatorATemperature", radiatorA, measuredAt);
        insertHistory("radiatorBTemperature", radiatorB, measuredAt);
    }

    private void insertHistory(String metricKey, double value, Instant measuredAt) {
        jdbcTemplate.update(
                """
                INSERT INTO measurement_history (metric_key, value, unit, measured_at)
                VALUES (?, ?, ?, ?)
                """,
                metricKey,
                value,
                MetricStatusEvaluator.ruleFor(metricKey).unit(),
                measuredAt.toString()
        );
    }
}
