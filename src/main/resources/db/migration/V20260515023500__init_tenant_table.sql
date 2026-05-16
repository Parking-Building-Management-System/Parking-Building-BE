CREATE TABLE tenants (
    -- PK & BaseEntity fields
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    email_contact VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uk_tenants_slug UNIQUE (slug)
);
CREATE INDEX idx_tenants_active_not_deleted ON tenants (status, is_deleted);
CREATE INDEX idx_tenants_slug ON tenants (slug);
