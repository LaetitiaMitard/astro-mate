package fr.astromate.infrastructure.persistence;

import fr.astromate.application.port.MeasurementRepository;
import fr.astromate.domain.model.AlertEpisode;
import fr.astromate.domain.model.MeasurementSnapshot;
import fr.astromate.domain.model.MetricStatus;
import fr.astromate.domain.model.TechnicalEvent;
import fr.astromate.domain.service.MetricStatusEvaluator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMeasurementRepository implements MeasurementRepository {
    private static final Set<String> LEGACY_INDICATORS =
            Set.of("oxygen", "carbonDioxide", "availableEnergy");
    private final JdbcTemplate jdbc;

    public JdbcMeasurementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<MeasurementSnapshot> findMeasurement(String metricKey) {
        return jdbc.query("""
                SELECT metric_key, system_id, metric_id, name, value, unit, measured_at
                FROM metric_measurement WHERE metric_key = ?
                """, JdbcStationRepository::mapMeasurement, metricKey).stream().findFirst();
    }

    @Override
    public String availability(String systemId) {
        return jdbc.queryForObject("SELECT availability FROM system_availability WHERE system_id = ?",
                String.class, systemId);
    }

    @Override
    public List<MeasurementSnapshot> onlineMeasurements() {
        return jdbc.query("""
                SELECT metric_key, system_id, metric_id, name, value, unit, measured_at
                FROM metric_measurement
                WHERE system_id IN (
                    SELECT system_id FROM system_availability WHERE availability = 'ONLINE'
                )
                """, JdbcStationRepository::mapMeasurement);
    }

    @Override
    public void saveMeasurement(String metricKey, double value, Instant measuredAt) {
        String unit = MetricStatusEvaluator.ruleFor(metricKey).unit();
        int updated = jdbc.update("""
                UPDATE metric_measurement SET value = ?, unit = ?, measured_at = ?
                WHERE metric_key = ?
                """, value, unit, measuredAt.toString(), metricKey);
        if (updated != 1) {
            throw new IllegalArgumentException("Métrique non initialisée : " + metricKey);
        }
        jdbc.update("""
                INSERT INTO measurement_history (metric_key, value, unit, measured_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(metric_key, measured_at) DO UPDATE
                SET value = excluded.value, unit = excluded.unit
                """, metricKey, value, unit, measuredAt.toString());
    }

    @Override
    public void clearStaleness(String metricKey) {
        jdbc.update("DELETE FROM measurement_staleness_episode WHERE metric_key = ?", metricKey);
    }

    @Override
    public boolean startStaleness(String metricKey, Instant startedAt) {
        return jdbc.update("""
                INSERT OR IGNORE INTO measurement_staleness_episode (metric_key, started_at)
                VALUES (?, ?)
                """, metricKey, startedAt.toString()) == 1;
    }

    @Override
    public boolean startInvalidData(String metricKey, Instant startedAt) {
        return jdbc.update("""
                INSERT OR IGNORE INTO measurement_data_quality_episode (metric_key, started_at)
                VALUES (?, ?)
                """, metricKey, startedAt.toString()) == 1;
    }

    @Override
    public boolean closeInvalidData(String metricKey) {
        return jdbc.update("DELETE FROM measurement_data_quality_episode WHERE metric_key = ?",
                metricKey) == 1;
    }

    @Override
    public Optional<AlertEpisode> openAlert(String metricKey) {
        return jdbc.query("""
                SELECT id, level FROM alert_episode WHERE metric_key = ? AND status = 'OPEN'
                """, (row, number) -> new AlertEpisode(row.getString("id"), row.getString("level")),
                metricKey).stream().findFirst();
    }

    @Override
    public void resolveAlert(String id, Instant resolvedAt) {
        int updated = jdbc.update("""
                UPDATE alert_episode SET status = 'RESOLVED', resolved_at = ?
                WHERE id = ? AND status = 'OPEN'
                """, resolvedAt.toString(), id);
        if (updated != 1) {
            throw new IllegalStateException("L’alerte ouverte a changé pendant sa résolution.");
        }
    }

    @Override
    public void createAlert(
            MeasurementSnapshot metric, MetricStatus level, String message, Instant detectedAt
    ) {
        jdbc.update("""
                INSERT INTO alert_episode
                    (id, system_id, metric_id, metric_key, level, message, status,
                     detected_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, NULL)
                """, UUID.randomUUID().toString(), metric.systemId(), metric.metricId(),
                metric.metricKey(), level.name(), message, detectedAt.toString());
    }

    @Override
    public void appendEvent(TechnicalEvent event) {
        jdbc.update("""
                INSERT INTO station_event
                    (id, system_id, level, category, source, message, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID().toString(), event.systemId(), event.level(), event.category(),
                event.metricKey(), event.message(), event.occurredAt().toString());
    }

    @Override
    public boolean changeAvailability(String systemId, String availability) {
        int updated = jdbc.update("""
                UPDATE system_availability SET availability = ?
                WHERE system_id = ? AND availability <> ?
                """, availability, systemId, availability);
        if (updated == 0 && jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_availability WHERE system_id = ?",
                Integer.class, systemId) == 0) {
            throw new IllegalArgumentException("Système non initialisé : " + systemId);
        }
        return updated != 0;
    }

    @Override
    public void updateCompatibilityProjection(String metricKey, double value, Instant measuredAt) {
        if (LEGACY_INDICATORS.contains(metricKey)) {
            updateLegacyIndicator(metricKey, value, measuredAt);
        } else if (metricKey.equals("radiatorATemperature")
                || metricKey.equals("radiatorBTemperature")) {
            double radiatorA = jdbc.queryForObject(
                    "SELECT value FROM metric_measurement WHERE metric_key = ?",
                    Double.class, "radiatorATemperature");
            double radiatorB = jdbc.queryForObject(
                    "SELECT value FROM metric_measurement WHERE metric_key = ?",
                    Double.class, "radiatorBTemperature");
            double average = BigDecimal.valueOf(radiatorA / 2 + radiatorB / 2)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
            updateLegacyIndicator("temperature", average, measuredAt);
        }
    }

    private void updateLegacyIndicator(String key, double value, Instant measuredAt) {
        int updated = jdbc.update("""
                UPDATE dashboard_indicator SET value = ?, unit = ?, measured_at = ?
                WHERE indicator_key = ?
                """, value, MetricStatusEvaluator.ruleFor(key).unit(), measuredAt.toString(), key);
        if (updated != 1) {
            throw new IllegalStateException(key.equals("temperature")
                    ? "Projection de compatibilité de température absente."
                    : "Projection de compatibilité absente : " + key);
        }
    }
}
