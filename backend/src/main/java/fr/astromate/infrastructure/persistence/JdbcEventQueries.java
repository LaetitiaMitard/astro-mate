package fr.astromate.infrastructure.persistence;

import fr.astromate.application.model.EventLogView;
import fr.astromate.application.model.SystemDetailView;
import fr.astromate.application.port.EventQueries;
import fr.astromate.domain.model.StationCatalog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEventQueries implements EventQueries {
    private final JdbcTemplate jdbcTemplate;

    public JdbcEventQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EventLogView findEvents(
            int page, int pageSize, Instant effectiveFrom, Instant effectiveTo,
            String level, String category, String query, String effectiveSort
    ) {
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
        List<SystemDetailView.Event> events;
        long totalElements;
        if (query != null && !query.isBlank()) {
            String normalizedQuery = query.toLowerCase(Locale.ROOT);
            List<SystemDetailView.Event> matchingEvents = jdbcTemplate.query(
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
        return new EventLogView(
                events,
                page,
                pageSize,
                totalElements,
                totalPages
        );
    }

    private SystemDetailView.Event mapEvent(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        String systemId = resultSet.getString("system_id");
        return new SystemDetailView.Event(
                resultSet.getString("id"),
                resultSet.getString("level"),
                resultSet.getString("category"),
                systemId,
                StationCatalog.systemName(systemId),
                resultSet.getString("source"),
                resultSet.getString("message"),
                resultSet.getString("occurred_at")
        );
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
