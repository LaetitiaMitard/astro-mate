package fr.astromate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class AlertLogService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> STATUSES = Set.of("OPEN", "RESOLVED");
    private static final Set<String> LEVELS = Set.of("INFO", "WARNING", "CRITICAL");
    private static final Set<String> SYSTEM_IDS = Set.of(
            "life-support",
            "thermal-control",
            "power-supply",
            "communications"
    );

    private final JdbcTemplate jdbcTemplate;

    AlertLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    AlertPageResponse getAlerts(
            int page,
            int pageSize,
            String status,
            String level,
            String systemId
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
        if (status != null && !STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L’état de l’alerte est invalide."
            );
        }
        if (level != null && !LEVELS.contains(level)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le niveau de l’alerte est invalide."
            );
        }
        if (systemId != null && !SYSTEM_IDS.contains(systemId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le système de l’alerte est invalide."
            );
        }

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
        List<DashboardResponse.Alert> alerts = jdbcTemplate.query(
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
        return new AlertPageResponse(
                alerts,
                page,
                pageSize,
                totalElements,
                totalPages
        );
    }

    private DashboardResponse.Alert mapAlert(
            ResultSet resultSet,
            int rowNumber
    ) throws SQLException {
        String systemId = resultSet.getString("system_id");
        return new DashboardResponse.Alert(
                resultSet.getString("id"),
                resultSet.getString("level"),
                systemId,
                SystemDetailService.systemName(systemId),
                resultSet.getString("metric_id"),
                resultSet.getString("message"),
                resultSet.getString("status"),
                resultSet.getString("detected_at"),
                resultSet.getString("resolved_at")
        );
    }
}
