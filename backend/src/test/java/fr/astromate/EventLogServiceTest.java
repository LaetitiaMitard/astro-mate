package fr.astromate;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventLogServiceTest {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private EventLogService service;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                """
                CREATE TABLE station_event (
                    id TEXT PRIMARY KEY,
                    system_id TEXT,
                    level TEXT NOT NULL,
                    category TEXT NOT NULL,
                    source TEXT,
                    message TEXT NOT NULL,
                    occurred_at TEXT NOT NULL
                )
                """
        );
        service = new EventLogService(jdbcTemplate);
    }

    @AfterEach
    void closeDataSource() {
        dataSource.destroy();
    }

    @Test
    void filtersEventsAndReturnsTheRequestedSortAndPageSize() {
        Instant now = Instant.now();
        insertEvent(
                "older-match",
                "thermal-control",
                "WARNING",
                "DATA",
                "simulation",
                "Radiateur A en surchauffe",
                now.minusSeconds(10)
        );
        insertEvent(
                "newer-match",
                "thermal-control",
                "WARNING",
                "DATA",
                "simulation",
                "Débit à vérifier",
                now.minusSeconds(5)
        );
        insertEvent(
                "wrong-level",
                "thermal-control",
                "INFO",
                "DATA",
                "simulation",
                "Contrôle thermique",
                now.minusSeconds(3)
        );

        EventLogResponse result = service.getEvents(
                0,
                1,
                now.minusSeconds(60).toString(),
                now.plusSeconds(1).toString(),
                "WARNING",
                "DATA",
                "contrôle thermique",
                "occurredAt,asc"
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).id()).isEqualTo("older-match");
        assertThat(result.page()).isZero();
        assertThat(result.pageSize()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidPageSizeAndSearchPeriod() {
        assertThatThrownBy(() -> service.getEvents(
                0, 101, null, null, null, null, null, null
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST)
                );

        assertThatThrownBy(() -> service.getEvents(
                0,
                20,
                "2026-10-21T12:00:00Z",
                "2026-10-21T11:00:00Z",
                null,
                null,
                null,
                null
        ))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST)
                );
    }

    @Test
    void searchesFrenchEventTextWithoutCaseSensitivity() {
        insertEvent(
                "accented-message",
                "thermal-control",
                "INFO",
                "SYSTEM",
                "simulation",
                "Événement thermique",
                Instant.now().minusSeconds(1)
        );

        EventLogResponse result = service.getEvents(
                0,
                20,
                null,
                null,
                null,
                null,
                "éVÉNEMENT",
                null
        );

        assertThat(result.items()).extracting(SystemDetailResponse.Event::id)
                .containsExactly("accented-message");
    }

    private void insertEvent(
            String id,
            String systemId,
            String level,
            String category,
            String source,
            String message,
            Instant occurredAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO station_event
                    (id, system_id, level, category, source, message, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                systemId,
                level,
                category,
                source,
                message,
                occurredAt.toString()
        );
    }
}
