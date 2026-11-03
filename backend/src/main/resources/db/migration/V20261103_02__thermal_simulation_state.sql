CREATE TABLE IF NOT EXISTS simulation_state (
    scenario_id TEXT PRIMARY KEY CHECK (scenario_id = 'thermal'),
    elapsed_millis INTEGER NOT NULL CHECK (elapsed_millis >= 0 AND elapsed_millis < 240000),
    initialized_at TEXT NOT NULL
);
