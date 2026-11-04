package fr.astromate;

import fr.astromate.application.model.ApplicationException;
import fr.astromate.application.model.EventLogView;
import fr.astromate.application.model.SystemDetailView;
import fr.astromate.application.service.EventLogService;
import fr.astromate.infrastructure.persistence.JdbcEventQueries;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

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
        service = new EventLogService(new JdbcEventQueries(jdbcTemplate), java.time.Clock.systemUTC());
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

        EventLogView result = service.getEvents(
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
                        ApplicationException.class,
                        exception -> assertThat(exception.kind())
                                .isEqualTo(ApplicationException.Kind.BAD_REQUEST)
                );

        assertThatThrownBy(() -> service.getEvents(
                0, 21, null, null, null, null, null, null
        ))
                .isInstanceOfSatisfying(
                        ApplicationException.class,
                        exception -> assertThat(exception.kind())
                                .isEqualTo(ApplicationException.Kind.BAD_REQUEST)
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
                        ApplicationException.class,
                        exception -> assertThat(exception.kind())
                                .isEqualTo(ApplicationException.Kind.BAD_REQUEST)
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

        EventLogView result = service.getEvents(
                0,
                20,
                null,
                null,
                null,
                null,
                "éVÉNEMENT",
                null
        );

        assertThat(result.items()).extracting(SystemDetailView.Event::id)
                .containsExactly("accented-message");
    }

    @Test
    void searchesEventSourceAndSystemNameWithoutCaseSensitivity() {
        Instant now = Instant.now();
        insertEvent(
                "source-match",
                null,
                "INFO",
                "SYSTEM",
                "Navigation Bus",
                "État stable",
                now.minusSeconds(2)
        );
        insertEvent(
                "system-match",
                "thermal-control",
                "WARNING",
                "DATA",
                "simulation",
                "Température relevée",
                now.minusSeconds(1)
        );

        EventLogView sourceResult = service.getEvents(
                0, 20, null, null, null, null, "nAVIGATION", null
        );
        EventLogView systemResult = service.getEvents(
                0, 20, null, null, null, null, "CONTRÔLE THERMIQUE", null
        );

        assertThat(sourceResult.items())
                .extracting(SystemDetailView.Event::id)
                .containsExactly("source-match");
        assertThat(systemResult.items())
                .extracting(SystemDetailView.Event::id)
                .containsExactly("system-match");
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
