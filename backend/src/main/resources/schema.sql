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

CREATE TABLE IF NOT EXISTS system_availability (
    system_id TEXT PRIMARY KEY,
    availability TEXT NOT NULL CHECK (availability IN ('ONLINE', 'OFFLINE'))
);
