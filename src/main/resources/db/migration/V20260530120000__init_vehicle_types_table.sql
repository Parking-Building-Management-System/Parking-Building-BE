CREATE TABLE vehicle_types (
    id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_vehicle_types PRIMARY KEY (id)
);

CREATE INDEX idx_vehicle_types_active_created ON vehicle_types (is_deleted, created_date DESC);
