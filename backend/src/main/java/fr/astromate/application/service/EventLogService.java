package fr.astromate.application.service;

import fr.astromate.application.model.ApplicationException;
import fr.astromate.application.model.EventLogView;
import fr.astromate.application.port.EventQueries;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;

public class EventLogService {
    private final EventQueries queries;
    private final Clock clock;

    public EventLogService(EventQueries queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    private static final Duration EVENT_WINDOW = Duration.ofDays(7);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 20;
    private static final String DEFAULT_SORT = "occurredAt,desc";
    private static final Set<String> LEVELS = Set.of("INFO", "WARNING", "CRITICAL");
    private static final Set<String> CATEGORIES = Set.of(
            "SYSTEM",
            "ALERT",
            "MAINTENANCE",
            "OPERATION",
            "DATA"
    );
    private static final Set<String> SORTS = Set.of(
            "occurredAt,desc",
            "occurredAt,asc"
    );

    public EventLogView getEvents(
            int page,
            int pageSize,
            String from,
            String to,
            String level,
            String category,
            String query,
            String sort
    ) {
        if (page < 0) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "Le numéro de page doit être positif ou nul."
            );
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "La taille de page doit être comprise entre 1 et 20."
            );
        }
        if (level != null && !LEVELS.contains(level)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "Le niveau d’événement est invalide."
            );
        }
        if (category != null && !CATEGORIES.contains(category)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "La catégorie d’événement est invalide."
            );
        }
        if (query != null && query.length() > 200) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "La recherche ne peut pas dépasser 200 caractères."
            );
        }
        String effectiveSort = sort == null ? DEFAULT_SORT : sort;
        if (!SORTS.contains(effectiveSort)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "Le tri demandé est invalide."
            );
        }

        Instant now = clock.instant();
        Instant requestedFrom = parseBound(from, "from");
        Instant requestedTo = parseBound(to, "to");
        Instant effectiveFrom = requestedFrom == null
                ? now.minus(EVENT_WINDOW)
                : requestedFrom;
        Instant effectiveTo = requestedTo == null ? now : requestedTo;
        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "Le début de période doit précéder sa fin."
            );
        }

        return queries.findEvents(
                page, pageSize, effectiveFrom, effectiveTo, level, category, query, effectiveSort);
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
                    "Le paramètre " + parameterName
                            + " doit être une date UTC ISO 8601.",
                    exception
            );
        }
    }
}
