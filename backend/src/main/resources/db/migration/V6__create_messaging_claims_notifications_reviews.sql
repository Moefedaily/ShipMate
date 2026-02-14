-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE claim_status AS ENUM (
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED',
    'PAID'
);

CREATE TYPE message_type AS ENUM (
    'TEXT',
    'SYSTEM',
    'LOCATION_UPDATE'
);

CREATE TYPE notification_type AS ENUM (
    'BOOKING_UPDATE',
    'PAYMENT_STATUS',
    'DELIVERY_STATUS',
    'NEW_MESSAGE',
    'SYSTEM_ALERT'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SUCCEEDED',
    'FAILED',
    'REFUNDED'
);

CREATE TYPE payment_method AS ENUM (
    'CARD',
    'PAYPAL',
    'BANK_TRANSFER'
);

-- ============================================================================
-- TABLES
-- ============================================================================

-- Insurance Claims Table
CREATE TABLE insurance_claims (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    CONSTRAINT fk_insurance_claims_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE RESTRICT,
    claimant_id UUID NOT NULL,
    CONSTRAINT fk_insurance_claims_claimant
        FOREIGN KEY (claimant_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    claim_amount NUMERIC(10,2) NOT NULL CHECK (claim_amount > 0),
    claim_description TEXT,
    claim_status claim_status NOT NULL,
    photos TEXT,
    admin_notes TEXT,
    admin_user_id UUID,
    CONSTRAINT fk_insurance_claims_admin_user
        FOREIGN KEY (admin_user_id)
        REFERENCES users(id)
        ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_insurance_claims_shipment_id ON insurance_claims(shipment_id);
CREATE INDEX idx_insurance_claims_claimant_id ON insurance_claims(claimant_id);
CREATE INDEX idx_insurance_claims_status ON insurance_claims(claim_status);

-- Messages Table
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    shipment_id UUID NOT NULL,
    CONSTRAINT fk_messages_shipment
        FOREIGN KEY (shipment_id)
        REFERENCES shipments(id)
        ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    receiver_id UUID NOT NULL,
    CONSTRAINT fk_messages_receiver
        FOREIGN KEY (receiver_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    message_content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    message_type message_type NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_shipment_id ON messages(shipment_id);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX idx_messages_is_read ON messages(is_read);

-- Notifications Table
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type notification_type NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_type ON notifications(notification_type);

-- Reviews Table
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    CONSTRAINT fk_reviews_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE RESTRICT,
    reviewer_id UUID NOT NULL,
    CONSTRAINT fk_reviews_reviewer
        FOREIGN KEY (reviewer_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    reviewed_id UUID NOT NULL,
    CONSTRAINT fk_reviews_reviewed
        FOREIGN KEY (reviewed_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reviews_booking_id ON reviews(booking_id);
CREATE INDEX idx_reviews_reviewer_id ON reviews(reviewer_id);
CREATE INDEX idx_reviews_reviewed_id ON reviews(reviewed_id);

-- Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE,
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(id)
        ON DELETE RESTRICT,
    stripe_payment_intent_id VARCHAR(255),
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    payment_status payment_status NOT NULL,
    payment_method payment_method NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_stripe_intent ON payments(stripe_payment_intent_id);