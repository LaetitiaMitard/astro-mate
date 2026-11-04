package fr.astromate.application.service;

import fr.astromate.application.model.ApplicationException;
import fr.astromate.application.model.MeasurementSeriesView;
import fr.astromate.application.model.MeasurementSeriesView.Measurement;
import fr.astromate.application.port.HistoryQueries;
import fr.astromate.domain.model.StationCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class MeasurementHistoryService {
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

    private final HistoryQueries queries;
    private final Clock clock;

    public MeasurementHistoryService(HistoryQueries queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    public MeasurementSeriesView getMeasurements(
            String systemId,
            String indicatorKey,
            String from,
            String to
    ) {
        SeriesMetadata metadata = SERIES.get(indicatorKey);
        if (metadata == null || !metadata.systemId().equals(systemId)) {
            throw new ApplicationException(
                    ApplicationException.Kind.NOT_FOUND,
                    "Indicateur inconnu pour ce système."
            );
        }

        Instant now = clock.instant();
        Instant requestedFrom = parseBound(from, "from");
        Instant requestedTo = parseBound(to, "to");
        if (requestedFrom != null
                && requestedTo != null
                && requestedFrom.isAfter(requestedTo)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
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
                ? queries.readTemperatureMeasurements(
                        metadata.metricKeys(), metadata.unit(), effectiveFrom, effectiveTo)
                : queries.readMetricMeasurements(
                        metadata.metricKeys().get(0),
                        metadata.unit(),
                        effectiveFrom,
                        effectiveTo
                );
        return new MeasurementSeriesView(
                indicatorKey,
                metadata.metricIds(),
                metadata.name(),
                metadata.unit(),
                effectiveFrom.toString(),
                effectiveTo.toString(),
                measurements
        );
    }

    private static Instant parseBound(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
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
        var metadata = StationCatalog.METRIC_METADATA.get(metricKey);
        if (metadata == null) {
            throw new IllegalStateException("Métrique inconnue dans une série : " + metricKey);
        }
        return metadata.id();
    }
}
