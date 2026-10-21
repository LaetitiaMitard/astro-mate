package fr.astromate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class EventLogService {
    private static final Duration EVENT_WINDOW = Duration.ofDays(7);
    private static final int PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    EventLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    EventLogResponse getEvents(int page) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le numéro de page doit être positif ou nul."
            );
        }

        Instant now = Instant.now();
        Instant from = now.minus(EVENT_WINDOW);
        long totalElements = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM station_event
                WHERE julianday(occurred_at) >= julianday(?)
                  AND julianday(occurred_at) <= julianday(?)
                """,
                Long.class,
                from.toString(),
                now.toString()
        );
        List<SystemDetailResponse.Event> events = jdbcTemplate.query(
                """
                SELECT id, level, category, system_id, source, message, occurred_at
                FROM station_event
                WHERE julianday(occurred_at) >= julianday(?)
                  AND julianday(occurred_at) <= julianday(?)
                ORDER BY julianday(occurred_at) DESC, id DESC
                LIMIT ? OFFSET ?
                """,
                (resultSet, rowNumber) -> new SystemDetailResponse.Event(
                        resultSet.getString("id"),
                        resultSet.getString("level"),
                        resultSet.getString("category"),
                        resultSet.getString("system_id"),
                        SystemDetailService.systemName(
                                resultSet.getString("system_id")
                        ),
                        resultSet.getString("source"),
                        resultSet.getString("message"),
                        resultSet.getString("occurred_at")
                ),
                from.toString(),
                now.toString(),
                PAGE_SIZE,
                page * (long) PAGE_SIZE
        );
        int totalPages = (int) ((totalElements + PAGE_SIZE - 1) / PAGE_SIZE);
        return new EventLogResponse(
                events,
                page,
                PAGE_SIZE,
                totalElements,
                totalPages
        );
    }
}
