CREATE TABLE bookings (
    id UUID PRIMARY KEY,

    driver_id UUID NOT NULL,
    CONSTRAINT fk_bookings_driver
        FOREIGN KEY (driver_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    status VARCHAR(30) NOT NULL,

    total_price NUMERIC(10,2),
    platform_commission NUMERIC(10,2),
    driver_earnings NUMERIC(10,2),

    current_weight NUMERIC(6,2),

    general_pickup_area VARCHAR(100),
    general_delivery_area VARCHAR(100),

    estimated_pickup_date DATE,
    estimated_delivery_date DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
