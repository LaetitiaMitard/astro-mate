package fr.astromate.infrastructure.persistence;

import fr.astromate.application.model.AlertPageView;
import fr.astromate.application.model.DashboardView;
import fr.astromate.application.port.AlertQueries;
import fr.astromate.domain.model.StationCatalog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAlertQueries implements AlertQueries {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAlertQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AlertPageView findAlerts(
            int page, int pageSize, String status, String level, String systemId
    ) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (status != null) {
            where.append(" AND status = ?");
            parameters.add(status);
        }
        if (level != null) {
            where.append(" AND level = ?");
            parameters.add(level);
        }
        if (systemId != null) {
            where.append(" AND system_id = ?");
            parameters.add(systemId);
        }

        long totalElements = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alert_episode" + where,
                Long.class,
                parameters.toArray()
        );
        List<Object> pageParameters = new ArrayList<>(parameters);
        pageParameters.add(pageSize);
        pageParameters.add(page * (long) pageSize);
        List<DashboardView.Alert> alerts = jdbcTemplate.query(
                """
                SELECT id, level, system_id, metric_id, message, status,
                       detected_at, resolved_at
                FROM alert_episode
                """ + where + " " + """
                ORDER BY CASE status WHEN 'OPEN' THEN 0 ELSE 1 END,
                         CASE level
                             WHEN 'CRITICAL' THEN 0
                             WHEN 'WARNING' THEN 1
                             ELSE 2
                         END,
                         julianday(detected_at) DESC, id
                LIMIT ? OFFSET ?
                """,
                this::mapAlert,
                pageParameters.toArray()
        );
        int totalPages = (int) ((totalElements + pageSize - 1) / pageSize);
        return new AlertPageView(
                alerts,
                page,
                pageSize,
                totalElements,
                totalPages
        );
    }

    private DashboardView.Alert mapAlert(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        String systemId = resultSet.getString("system_id");
        return new DashboardView.Alert(
                resultSet.getString("id"),
                resultSet.getString("level"),
                systemId,
                StationCatalog.systemName(systemId),
                resultSet.getString("metric_id"),
                resultSet.getString("message"),
                resultSet.getString("status"),
                resultSet.getString("detected_at"),
                resultSet.getString("resolved_at")
        );
    }

    @Override
    public List<DashboardView.Alert> recentOpenAlerts() {
        return jdbcTemplate.query(
                """
                SELECT id, level, system_id, metric_id, message, status,
                       detected_at, resolved_at
                FROM alert_episode
                WHERE status = 'OPEN'
                ORDER BY CASE level WHEN 'CRITICAL' THEN 0 ELSE 1 END,
                         detected_at DESC
                LIMIT 4
                """,
                (resultSet, rowNumber) -> new DashboardView.Alert(
                        resultSet.getString("id"),
                        resultSet.getString("level"),
                        resultSet.getString("system_id"),
                        systemName(resultSet.getString("system_id")),
                        resultSet.getString("metric_id"),
                        resultSet.getString("message"),
                        resultSet.getString("status"),
                        resultSet.getString("detected_at"),
                        resultSet.getString("resolved_at")
                )
        );
    }

    private static String systemName(String systemId) {
        String name = StationCatalog.systemName(systemId);
        if (name == null) {
            throw new IllegalStateException("Système inattendu dans une alerte : " + systemId);
        }
        return name;
    }
}
