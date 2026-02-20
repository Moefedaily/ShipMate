
CREATE TYPE payout_status AS ENUM (
    'PENDING',
    'PAID'
);

CREATE TYPE earning_type AS ENUM (
    'ORIGINAL',
    'REFUND'
);

CREATE TABLE driver_earnings (
    id UUID PRIMARY KEY,

    driver_id UUID NOT NULL,
    CONSTRAINT fk_driver_earnings_driver
        FOREIGN KEY (driver_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    shipment_id UUID NOT NULL,
    CONSTRAINT fk_driver_earnings_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE RESTRICT,

    payment_id UUID NOT NULL,
    CONSTRAINT fk_driver_earnings_payment
        FOREIGN KEY (payment_id)
        REFERENCES payments(id)
        ON DELETE RESTRICT,

    earning_type earning_type NOT NULL,

    gross_amount NUMERIC(10,2) NOT NULL,
    commission_amount NUMERIC(10,2) NOT NULL,
    net_amount NUMERIC(10,2) NOT NULL,

    CONSTRAINT driver_earnings_amount_consistency_check
        CHECK (gross_amount = commission_amount + net_amount),

    payout_status payout_status NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_driver_earnings_payment_type
    ON driver_earnings(payment_id, earning_type);

CREATE INDEX idx_driver_earnings_driver_id
    ON driver_earnings(driver_id);

CREATE INDEX idx_driver_earnings_payout_status
    ON driver_earnings(payout_status);
