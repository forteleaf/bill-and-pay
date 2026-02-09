-- Fee configuration change history table
CREATE TABLE fee_configuration_history (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    fee_configuration_id UUID NOT NULL REFERENCES fee_configurations(id),
    merchant_id UUID REFERENCES merchants(id),
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATE', 'UPDATE', 'DEACTIVATE', 'ACTIVATE')),
    field_name VARCHAR(50),
    old_value TEXT,
    new_value TEXT,
    old_fee_rate NUMERIC(10,6),
    new_fee_rate NUMERIC(10,6),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    reason TEXT,
    changed_by VARCHAR(100),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fee_config_history_config_id ON fee_configuration_history(fee_configuration_id);
CREATE INDEX idx_fee_config_history_merchant_id ON fee_configuration_history(merchant_id);
CREATE INDEX idx_fee_config_history_changed_at ON fee_configuration_history(changed_at DESC);
