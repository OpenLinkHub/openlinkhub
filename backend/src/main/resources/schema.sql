CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS olh_product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL UNIQUE,
    category VARCHAR(80) NOT NULL DEFAULT 'general',
    description TEXT,
    thing_model JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS olh_device (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES olh_product(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    device_key VARCHAR(120) NOT NULL UNIQUE,
    secret VARCHAR(120) NOT NULL,
    location VARCHAR(160),
    status VARCHAR(24) NOT NULL DEFAULT 'offline',
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS olh_sensor (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT NOT NULL REFERENCES olh_device(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    sensor_key VARCHAR(120) NOT NULL,
    sensor_type VARCHAR(40) NOT NULL DEFAULT 'number',
    unit VARCHAR(40),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (device_id, sensor_key)
);

CREATE TABLE IF NOT EXISTS olh_telemetry_data (
    time TIMESTAMPTZ NOT NULL,
    device_id BIGINT NOT NULL REFERENCES olh_device(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES olh_product(id) ON DELETE CASCADE,
    metric VARCHAR(120) NOT NULL,
    numeric_value DOUBLE PRECISION,
    text_value TEXT,
    value_type VARCHAR(24) NOT NULL,
    quality VARCHAR(24) NOT NULL DEFAULT 'good',
    raw_value JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable('olh_telemetry_data', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_telemetry_device_metric_time
    ON olh_telemetry_data (device_id, metric, time DESC);

CREATE TABLE IF NOT EXISTS olh_telemetry_latest (
    device_id BIGINT NOT NULL REFERENCES olh_device(id) ON DELETE CASCADE,
    metric VARCHAR(120) NOT NULL,
    product_id BIGINT NOT NULL REFERENCES olh_product(id) ON DELETE CASCADE,
    time TIMESTAMPTZ NOT NULL,
    numeric_value DOUBLE PRECISION,
    text_value TEXT,
    value_type VARCHAR(24) NOT NULL,
    quality VARCHAR(24) NOT NULL DEFAULT 'good',
    raw_value JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (device_id, metric)
);

CREATE TABLE IF NOT EXISTS olh_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(140) NOT NULL,
    device_key VARCHAR(120),
    metric VARCHAR(120) NOT NULL,
    operator VARCHAR(12) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    severity VARCHAR(24) NOT NULL DEFAULT 'warning',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS olh_alarm (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT REFERENCES olh_rule(id) ON DELETE SET NULL,
    device_id BIGINT REFERENCES olh_device(id) ON DELETE SET NULL,
    device_key VARCHAR(120) NOT NULL,
    metric VARCHAR(120) NOT NULL,
    value DOUBLE PRECISION,
    threshold DOUBLE PRECISION,
    operator VARCHAR(12),
    severity VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'open',
    message TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alarm_status_time ON olh_alarm (status, occurred_at DESC);

INSERT INTO olh_product (name, code, category, description, thing_model)
VALUES (
    'Smart Environment Sensor',
    'env-sensor',
    'sensor',
    'Default product for HTTP telemetry demos.',
    '[{"key":"temperature","name":"Temperature","type":"number","unit":"C"},{"key":"humidity","name":"Humidity","type":"number","unit":"%"},{"key":"voltage","name":"Voltage","type":"number","unit":"V"}]'::jsonb
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO olh_device (product_id, name, device_key, secret, location, status)
SELECT id, 'Demo Sensor 001', 'demo-device-001', 'demo-secret', 'Lab / Rack A', 'offline'
FROM olh_product
WHERE code = 'env-sensor'
ON CONFLICT (device_key) DO NOTHING;

INSERT INTO olh_sensor (device_id, name, sensor_key, sensor_type, unit, description)
SELECT d.id, sensor.name, sensor.sensor_key, sensor.sensor_type, sensor.unit, sensor.description
FROM olh_device d
CROSS JOIN (
    VALUES
        ('GPS Location', 'gps', 'string', NULL, 'GPS or installation location payload.'),
        ('Temperature Sensor', 'temperature', 'number', 'C', 'Temperature telemetry.'),
        ('Humidity Sensor', 'humidity', 'number', '%', 'Humidity telemetry.'),
        ('Battery Level', 'battery', 'number', '%', 'Battery level telemetry.'),
        ('Voltage Sensor', 'voltage', 'number', 'V', 'Voltage telemetry.')
) AS sensor(name, sensor_key, sensor_type, unit, description)
WHERE d.device_key = 'demo-device-001'
ON CONFLICT (device_id, sensor_key) DO NOTHING;

INSERT INTO olh_rule (name, device_key, metric, operator, threshold, severity, description)
VALUES (
    'High temperature guard',
    'demo-device-001',
    'temperature',
    '>',
    35,
    'critical',
    'Create an alarm when demo sensor temperature is above 35C.'
)
ON CONFLICT DO NOTHING;
