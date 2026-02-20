CREATE TYPE shipment_status AS ENUM (
    'CREATED',
    'ASSIGNED',
    'IN_TRANSIT',
    'DELIVERED',
    'CANCELLED'
);

CREATE TABLE shipments (
    id UUID PRIMARY KEY,

    sender_id UUID NOT NULL,
    CONSTRAINT fk_shipments_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id),

    pickup_address TEXT NOT NULL,
    pickup_latitude DECIMAL(9,6) NOT NULL,
    pickup_longitude DECIMAL(9,6) NOT NULL,

    delivery_address TEXT NOT NULL,
    delivery_latitude DECIMAL(9,6) NOT NULL,
    delivery_longitude DECIMAL(9,6) NOT NULL,

    package_description TEXT,
    package_weight DECIMAL(6,2) NOT NULL CHECK (package_weight > 0),
    package_value DECIMAL(10,2) NOT NULL CHECK (package_value >= 0),

    requested_pickup_date DATE NOT NULL,
    requested_delivery_date DATE NOT NULL,
    CHECK (requested_pickup_date <= requested_delivery_date),

    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',

    base_price DECIMAL(8,2) NOT NULL CHECK (base_price >= 0),

    pickup_order INT,
    delivery_order INT,
    extra_insurance_fee DECIMAL(8,2) DEFAULT 0 CHECK (extra_insurance_fee >= 0),
    photos JSONB,
    delivery_code_hash TEXT,
    delivery_code_salt TEXT,
    delivery_code_created_at TIMESTAMPTZ,
    delivery_code_verified_at TIMESTAMPTZ,
    delivery_code_attempts INT,
    booking_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shipments_sender_id ON shipments(sender_id);
CREATE INDEX idx_shipments_status ON shipments(status);
