package fr.astromate;

import fr.astromate.MeasurementSeriesResponse.Measurement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class MeasurementHistoryService {
    private static final Duration HISTORY_WINDOW = Duration.ofHours(6);
    private static final Map<String, SeriesMetadata> SERIES = Map.of(
            "oxygen",
            new SeriesMetadata(
                    "life-support",
                    List.of("oxygen"),
                    "O₂",
                    "% vol."
            ),
            "carbonDioxide",
            new SeriesMetadata(
                    "life-support",
                    List.of("carbonDioxide"),
                    "CO₂",
                    "ppm"
            ),
            "temperature",
            new SeriesMetadata(
                    "thermal-control",
                    List.of("radiatorATemperature", "radiatorBTemperature"),
                    "Température",
                    "°C"
            ),
            "availableEnergy",
            new SeriesMetadata(
                    "power-supply",
                    List.of("availableEnergy"),
                    "Énergie disponible",
                    "%"
            ),
            "fluidFlow",
            new SeriesMetadata(
                    "thermal-control",
                    List.of("fluidFlow"),
                    "Débit de fluide",
                    "L/min"
            ),
            "pressure",
            new SeriesMetadata(
                    "thermal-control",
                    List.of("pressure"),
                    "Pression",
                    "bar"
            ),
            "consumption",
            new SeriesMetadata(
                    "thermal-control",
                    List.of("thermalConsumption"),
                    "Consommation thermique",
                    "kW"
            )
    );

    private final JdbcTemplate jdbcTemplate;

    MeasurementHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    MeasurementSeriesResponse getMeasurements(
            String systemId,
            String indicatorKey,
            String from,
            String to
    ) {
        SeriesMetadata metadata = SERIES.get(indicatorKey);
        if (metadata == null || !metadata.systemId().equals(systemId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Indicateur inconnu pour ce système."
            );
        }

        Instant now = Instant.now();
        Instant requestedFrom = parseBound(from, "from");
        Instant requestedTo = parseBound(to, "to");
        if (requestedFrom != null
                && requestedTo != null
                && requestedFrom.isAfter(requestedTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le début de période doit précéder sa fin."
            );
        }

        Instant earliest = now.minus(HISTORY_WINDOW);
        Instant effectiveFrom = clamp(
                requestedFrom == null ? earliest : requestedFrom,
                earliest,
                now
        );
        Instant effectiveTo = clamp(
                requestedTo == null ? now : requestedTo,
                earliest,
                now
        );
        if (effectiveFrom.isAfter(effectiveTo)) {
            effectiveFrom = effectiveTo;
        }

        List<Measurement> measurements = metadata.metricKeys().size() == 2
                ? readTemperatureMeasurements(metadata, effectiveFrom, effectiveTo)
                : readMetricMeasurements(
                        metadata.metricKeys().get(0),
                        metadata.unit(),
                        effectiveFrom,
                        effectiveTo
                );
        return new MeasurementSeriesResponse(
                indicatorKey,
                metadata.metricIds(),
                metadata.name(),
                metadata.unit(),
                effectiveFrom.toString(),
                effectiveTo.toString(),
                measurements
        );
    }

    private List<Measurement> readMetricMeasurements(
            String metricKey,
            String unit,
            Instant from,
            Instant to
    ) {
        return jdbcTemplate.query(
                """
                SELECT value, measured_at
                FROM measurement_history
                WHERE metric_key = ?
                  AND unit = ?
                  AND julianday(measured_at) >= julianday(?)
                  AND julianday(measured_at) < julianday(?)
                ORDER BY julianday(measured_at), measured_at
                """,
                (resultSet, rowNumber) -> new Measurement(
                        resultSet.getDouble("value"),
                        resultSet.getString("measured_at")
                ),
                metricKey,
                unit,
                from.toString(),
                to.toString()
        );
    }

    private List<Measurement> readTemperatureMeasurements(
            SeriesMetadata metadata,
            Instant from,
            Instant to
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    (radiator_a.value / 2.0 + radiator_b.value / 2.0) AS value,
                    radiator_a.measured_at AS measured_at
                FROM measurement_history radiator_a
                JOIN measurement_history radiator_b
                  ON radiator_b.metric_key = ?
                 AND radiator_b.measured_at = radiator_a.measured_at
                WHERE radiator_a.metric_key = ?
                  AND radiator_a.unit = ?
                  AND radiator_b.unit = ?
                  AND julianday(radiator_a.measured_at) >= julianday(?)
                  AND julianday(radiator_a.measured_at) < julianday(?)
                ORDER BY julianday(radiator_a.measured_at), radiator_a.measured_at
                """,
                (resultSet, rowNumber) -> new Measurement(
                        BigDecimal.valueOf(resultSet.getDouble("value"))
                                .setScale(1, RoundingMode.HALF_UP)
                                .doubleValue(),
                        resultSet.getString("measured_at")
                ),
                metadata.metricKeys().get(1),
                metadata.metricKeys().get(0),
                metadata.unit(),
                metadata.unit(),
                from.toString(),
                to.toString()
        );
    }

    private static Instant parseBound(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le paramètre " + parameterName + " doit être une date UTC ISO 8601.",
                    exception
            );
        }
    }

    private static Instant clamp(Instant value, Instant minimum, Instant maximum) {
        if (value.isBefore(minimum)) {
            return minimum;
        }
        return value.isAfter(maximum) ? maximum : value;
    }

    private record SeriesMetadata(
            String systemId,
            List<String> metricKeys,
            String name,
            String unit
    ) {
        private List<String> metricIds() {
            return metricKeys.stream()
                    .map(MeasurementHistoryService::metricId)
                    .toList();
        }
    }

    private static String metricId(String metricKey) {
        return switch (metricKey) {
            case "oxygen" -> "oxygen-level";
            case "carbonDioxide" -> "carbon-dioxide-level";
            case "radiatorATemperature" -> "radiator-a-temperature";
            case "radiatorBTemperature" -> "radiator-b-temperature";
            case "availableEnergy" -> "available-energy";
            case "fluidFlow" -> "fluid-flow";
            case "pressure" -> "pressure";
            case "thermalConsumption" -> "thermal-consumption";
            default -> throw new IllegalStateException(
                    "Métrique inconnue dans une série : " + metricKey
            );
        };
    }
}
