package fr.astromate.application.port;

import fr.astromate.application.model.EventLogView;
import java.time.Instant;

public interface EventQueries {
    EventLogView findEvents(
            int page, int pageSize, Instant from, Instant to,
            String level, String category, String query, String sort
    );
}
