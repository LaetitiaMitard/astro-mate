INSERT OR IGNORE INTO dashboard_indicator
    (indicator_key, system_id, metric_id, name, value, unit, nominal_min, nominal_max, measured_at, display_order)
VALUES
    ('oxygen', 'life-support', 'oxygen-level', 'O₂', 21, '% vol.', 19, 23, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 1),
    ('carbonDioxide', 'life-support', 'carbon-dioxide-level', 'CO₂', 400, 'ppm', 0, 1000, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 2),
    ('temperature', 'thermal-control', 'system-temperature', 'Température', 22.3, '°C', 18, 26, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 3),
    ('availableEnergy', 'power-supply', 'available-energy', 'Énergie disponible', 78, '%', 20, 100, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 4);
