-- Version V20261103_01: persist the active invalid-data episode per metric.
-- Additive and idempotent; no historical data backfill is required.
-- Rollback after stopping this application version: DROP TABLE measurement_data_quality_episode.
CREATE TABLE IF NOT EXISTS measurement_data_quality_episode (
    metric_key TEXT PRIMARY KEY,
    started_at TEXT NOT NULL
);
