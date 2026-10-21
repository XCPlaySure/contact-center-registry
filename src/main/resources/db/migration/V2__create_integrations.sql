CREATE TABLE integrations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    display_name VARCHAR(120) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    credential_ref VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_integrations_tenant_display_name UNIQUE (tenant_id, display_name)
);

CREATE INDEX idx_integrations_tenant_status_created
    ON integrations (tenant_id, status, created_at DESC);