CREATE TABLE IF NOT EXISTS dashboard_indicator (
    indicator_key TEXT PRIMARY KEY,
    system_id TEXT NOT NULL,
    metric_id TEXT NOT NULL,
    name TEXT NOT NULL,
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    nominal_min REAL NOT NULL,
    nominal_max REAL NOT NULL,
    measured_at TEXT NOT NULL,
    display_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS metric_measurement (
    metric_key TEXT PRIMARY KEY,
    system_id TEXT NOT NULL,
    metric_id TEXT NOT NULL,
    name TEXT NOT NULL,
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    measured_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS measurement_history (
    metric_key TEXT NOT NULL,
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    measured_at TEXT NOT NULL,
    PRIMARY KEY (metric_key, measured_at)
);

CREATE INDEX IF NOT EXISTS measurement_history_measured_at
    ON measurement_history(measured_at);

CREATE TABLE IF NOT EXISTS system_availability (
    system_id TEXT PRIMARY KEY,
    availability TEXT NOT NULL CHECK (availability IN ('ONLINE', 'OFFLINE'))
);

CREATE TABLE IF NOT EXISTS alert_episode (
    id TEXT PRIMARY KEY,
    system_id TEXT NOT NULL,
    metric_id TEXT,
    metric_key TEXT NOT NULL,
    level TEXT NOT NULL CHECK (level IN ('INFO', 'WARNING', 'CRITICAL')),
    message TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    detected_at TEXT NOT NULL,
    resolved_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS one_open_alert_per_metric
    ON alert_episode(metric_key)
    WHERE status = 'OPEN';

CREATE TABLE IF NOT EXISTS station_event (
    id TEXT PRIMARY KEY,
    system_id TEXT,
    level TEXT NOT NULL CHECK (level IN ('INFO', 'WARNING', 'CRITICAL')),
    category TEXT NOT NULL CHECK (
        category IN ('SYSTEM', 'ALERT', 'MAINTENANCE', 'OPERATION', 'DATA')
    ),
    source TEXT,
    message TEXT NOT NULL,
    occurred_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS station_event_system_occurred
    ON station_event(system_id, occurred_at DESC);
