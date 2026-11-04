package fr.astromate.application.service;

import fr.astromate.application.model.AlertPageView;
import fr.astromate.application.model.ApplicationException;
import fr.astromate.application.port.AlertQueries;
import fr.astromate.domain.model.StationCatalog;
import java.util.Set;

public class AlertLogService {
    private final AlertQueries queries;

    public AlertLogService(AlertQueries queries) {
        this.queries = queries;
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> STATUSES = Set.of("OPEN", "RESOLVED");
    private static final Set<String> LEVELS = Set.of("INFO", "WARNING", "CRITICAL");

    public AlertPageView getAlerts(
            int page,
            int pageSize,
            String status,
            String level,
            String systemId
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
                    "La taille de page doit être comprise entre 1 et 100."
            );
        }
        if (status != null && !STATUSES.contains(status)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "L’état de l’alerte est invalide."
            );
        }
        if (level != null && !LEVELS.contains(level)) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "Le niveau de l’alerte est invalide."
            );
        }
        if (systemId != null && StationCatalog.systemName(systemId) == null) {
            throw new ApplicationException(
                    ApplicationException.Kind.BAD_REQUEST,
                    "Le système de l’alerte est invalide."
            );
        }

        return queries.findAlerts(page, pageSize, status, level, systemId);
    }
}
