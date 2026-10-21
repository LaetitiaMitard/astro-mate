package fr.astromate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class EventLogService {
    private static final Duration EVENT_WINDOW = Duration.ofDays(7);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
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

    private final JdbcTemplate jdbcTemplate;

    EventLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    EventLogResponse getEvents(
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le numéro de page doit être positif ou nul."
            );
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La taille de page doit être comprise entre 1 et 100."
            );
        }
        if (level != null && !LEVELS.contains(level)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le niveau d’événement est invalide."
            );
        }
        if (category != null && !CATEGORIES.contains(category)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La catégorie d’événement est invalide."
            );
        }
        if (query != null && query.length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La recherche ne peut pas dépasser 200 caractères."
            );
        }
        String effectiveSort = sort == null ? DEFAULT_SORT : sort;
        if (!SORTS.contains(effectiveSort)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le tri demandé est invalide."
            );
        }

        Instant now = Instant.now();
        Instant requestedFrom = parseBound(from, "from");
        Instant requestedTo = parseBound(to, "to");
        Instant effectiveFrom = requestedFrom == null
                ? now.minus(EVENT_WINDOW)
                : requestedFrom;
        Instant effectiveTo = requestedTo == null ? now : requestedTo;
        if (!effectiveFrom.isBefore(effectiveTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le début de période doit précéder sa fin."
            );
        }

        StringBuilder where = new StringBuilder(
                """
                WHERE julianday(occurred_at) >= julianday(?)
                  AND julianday(occurred_at) < julianday(?)
                """
        );
        List<Object> parameters = new ArrayList<>(
                List.of(effectiveFrom.toString(), effectiveTo.toString())
        );
        if (level != null) {
            where.append(" AND level = ?");
            parameters.add(level);
        }
        if (category != null) {
            where.append(" AND category = ?");
            parameters.add(category);
        }
        String direction = effectiveSort.endsWith(",asc") ? "ASC" : "DESC";
        String select = """
                SELECT id, level, category, system_id, source, message, occurred_at
                FROM station_event
                %s
                ORDER BY julianday(occurred_at) %s, id %s
                """.formatted(where, direction, direction);
        List<SystemDetailResponse.Event> events;
        long totalElements;
        if (query != null && !query.isBlank()) {
            String normalizedQuery = query.toLowerCase(Locale.ROOT);
            List<SystemDetailResponse.Event> matchingEvents = jdbcTemplate.query(
                    select,
                    this::mapEvent,
                    parameters.toArray()
            ).stream()
                    .filter(event -> containsIgnoreCase(
                            event.message(),
                            normalizedQuery
                    ) || containsIgnoreCase(
                            event.source(),
                            normalizedQuery
                    ) || containsIgnoreCase(
                            event.systemName(),
                            normalizedQuery
                    ))
                    .toList();
            totalElements = matchingEvents.size();
            long offset = page * (long) pageSize;
            events = offset >= totalElements
                    ? List.of()
                    : matchingEvents.stream()
                            .skip(offset)
                            .limit(pageSize)
                            .toList();
        } else {
            totalElements = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM station_event " + where,
                    Long.class,
                    parameters.toArray()
            );
            List<Object> pageParameters = new ArrayList<>(parameters);
            pageParameters.add(pageSize);
            pageParameters.add(page * (long) pageSize);
            events = jdbcTemplate.query(
                    select + " LIMIT ? OFFSET ?",
                    this::mapEvent,
                    pageParameters.toArray()
            );
        }
        int totalPages = (int) ((totalElements + pageSize - 1) / pageSize);
        return new EventLogResponse(
                events,
                page,
                pageSize,
                totalElements,
                totalPages
        );
    }

    private SystemDetailResponse.Event mapEvent(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        String systemId = resultSet.getString("system_id");
        return new SystemDetailResponse.Event(
                resultSet.getString("id"),
                resultSet.getString("level"),
                resultSet.getString("category"),
                systemId,
                SystemDetailService.systemName(systemId),
                resultSet.getString("source"),
                resultSet.getString("message"),
                resultSet.getString("occurred_at")
        );
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
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
                    "Le paramètre " + parameterName
                            + " doit être une date UTC ISO 8601.",
                    exception
            );
        }
    }
}
