package fr.astromate.application.port;

import fr.astromate.application.model.AlertPageView;
import fr.astromate.application.model.DashboardView;
import java.util.List;

public interface AlertQueries {
    List<DashboardView.Alert> recentOpenAlerts();

    AlertPageView findAlerts(int page, int pageSize, String status, String level, String systemId);
}
