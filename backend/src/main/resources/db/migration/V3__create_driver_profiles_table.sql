CREATE TABLE driver_profiles (
    id UUID NOT NULL,

    user_id UUID NOT NULL,

    license_number VARCHAR(50) NOT NULL,

    vehicle_type VARCHAR(30) NOT NULL,

    max_weight_capacity NUMERIC(6,2) NOT NULL,

    vehicle_description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    approved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_driver_profiles PRIMARY KEY (id),

    CONSTRAINT uq_driver_profiles_user UNIQUE (user_id),

    CONSTRAINT uq_driver_profiles_license UNIQUE (license_number),

    CONSTRAINT fk_driver_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);
