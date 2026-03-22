CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS monitors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    check_interval INTEGER NOT NULL,
    expected_status_code INTEGER DEFAULT 200,
    expected_keyword VARCHAR(255),
    timeout_ms INTEGER DEFAULT 10000,
    http_method VARCHAR(10) DEFAULT 'GET',
    headers TEXT,
    request_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS monitor_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID REFERENCES monitors(id) ON DELETE CASCADE,
    status INTEGER NOT NULL,
    response_time INTEGER NOT NULL,
    ssl_expiry_days INTEGER,
    checked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS maintenance_windows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID REFERENCES monitors(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL,
    start_hour INTEGER NOT NULL,
    start_minute INTEGER NOT NULL,
    end_hour INTEGER NOT NULL,
    end_minute INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_monitor_logs_monitor_id ON monitor_logs(monitor_id);

CREATE TABLE IF NOT EXISTS incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID REFERENCES monitors(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    error_cause VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS alert_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type VARCHAR(50) NOT NULL,
    destination VARCHAR(1024) NOT NULL
);

CREATE TABLE IF NOT EXISTS monitor_alerts (
    monitor_id UUID REFERENCES monitors(id) ON DELETE CASCADE,
    contact_id UUID REFERENCES alert_contacts(id) ON DELETE CASCADE,
    PRIMARY KEY (monitor_id, contact_id)
);
