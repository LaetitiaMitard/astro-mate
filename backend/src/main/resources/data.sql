INSERT OR IGNORE INTO dashboard_indicator
    (indicator_key, system_id, metric_id, name, value, unit, nominal_min, nominal_max, measured_at, display_order)
VALUES
    ('oxygen', 'life-support', 'oxygen-level', 'O₂', 21, '% vol.', 19, 23, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 1),
    ('carbonDioxide', 'life-support', 'carbon-dioxide-level', 'CO₂', 0.04 * 10000, 'ppm', 0, 1000, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 2),
    ('temperature', 'thermal-control', 'system-temperature', 'Température', 22.3, '°C', 18, 26, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 3),
    ('availableEnergy', 'power-supply', 'available-energy', 'Énergie disponible', 78, '%', 20, 100, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 4);

-- 1 % vol. = 10 000 ppm, donc 0,04 % vol. = 400 ppm.
INSERT OR IGNORE INTO metric_measurement
    (metric_key, system_id, metric_id, name, value, unit, measured_at)
SELECT indicator_key, system_id, metric_id, name, value, unit, measured_at
FROM dashboard_indicator
WHERE indicator_key IN ('oxygen', 'carbonDioxide', 'availableEnergy');

INSERT OR IGNORE INTO metric_measurement
    (metric_key, system_id, metric_id, name, value, unit, measured_at)
VALUES
    ('radiatorATemperature', 'thermal-control', 'radiator-a-temperature', 'Température Radiateur A', 22.1, '°C', strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    ('radiatorBTemperature', 'thermal-control', 'radiator-b-temperature', 'Température Radiateur B', 22.4, '°C', strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    ('fluidFlow', 'thermal-control', 'fluid-flow', 'Débit de fluide', 120, 'L/min', strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    ('pressure', 'thermal-control', 'pressure', 'Pression', 2.1, 'bar', strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    ('thermalConsumption', 'thermal-control', 'thermal-consumption', 'Consommation thermique', 1.2, 'kW', strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));

INSERT OR IGNORE INTO system_availability (system_id, availability)
VALUES
    ('life-support', 'ONLINE'),
    ('thermal-control', 'ONLINE'),
    ('power-supply', 'ONLINE'),
    ('communications', 'ONLINE');

WITH RECURSIVE sample_points(sample_index) AS (
    SELECT 0
    UNION ALL
    SELECT sample_index + 1
    FROM sample_points
    WHERE sample_index < 2160
),
history_values(metric_key, value, unit) AS (
    VALUES
        ('oxygen', 21, '% vol.'),
        ('carbonDioxide', 400, 'ppm'),
        ('radiatorATemperature', 22.1, '°C'),
        ('radiatorBTemperature', 22.4, '°C'),
        ('availableEnergy', 78, '%'),
        ('fluidFlow', 120, 'L/min'),
        ('pressure', 2.1, 'bar'),
        ('thermalConsumption', 1.2, 'kW')
)
INSERT OR IGNORE INTO measurement_history (metric_key, value, unit, measured_at)
SELECT
    history_values.metric_key,
    history_values.value,
    history_values.unit,
    strftime(
        '%Y-%m-%dT%H:%M:%fZ',
        'now',
        printf('%+d seconds', (sample_points.sample_index - 2160) * 10)
    )
FROM sample_points
CROSS JOIN history_values
WHERE NOT EXISTS (
    SELECT 1
    FROM measurement_history existing
    WHERE existing.metric_key = history_values.metric_key
);
