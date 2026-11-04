package fr.astromate.application.port;

import fr.astromate.application.model.SystemDetailView;
import java.time.Instant;
import java.util.List;

public interface SystemDetailQueries {
    List<SystemDetailView.Event> recentEvents(String systemId);

    Instant simulatedStateStartedAt();
}
