CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS monitors (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    check_interval INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS monitor_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    monitor_id UUID REFERENCES monitors(id) ON DELETE CASCADE,
    status INTEGER NOT NULL,
    response_time INTEGER NOT NULL,
    checked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_monitor_logs_monitor_id ON monitor_logs(monitor_id);
