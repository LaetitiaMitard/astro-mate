-- Version V20261103_03: track metrics that have crossed the freshness limit.
-- Additive and idempotent; no historical data backfill is required.
-- Rollback after stopping this application version: DROP TABLE measurement_staleness_episode.
CREATE TABLE IF NOT EXISTS measurement_staleness_episode (
    metric_key TEXT PRIMARY KEY,
    started_at TEXT NOT NULL
);
