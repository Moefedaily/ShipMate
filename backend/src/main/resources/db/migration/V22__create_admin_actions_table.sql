CREATE TABLE admin_actions (
    id UUID PRIMARY KEY,

    admin_id UUID NOT NULL,

    target_type VARCHAR(50) NOT NULL,
    target_id UUID NOT NULL,

    action VARCHAR(100) NOT NULL,

    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_actions_target
ON admin_actions(target_type, target_id);

CREATE INDEX idx_admin_actions_admin
ON admin_actions(admin_id);