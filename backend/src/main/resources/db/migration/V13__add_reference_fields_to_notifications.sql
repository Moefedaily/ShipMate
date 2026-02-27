CREATE TYPE reference_type AS ENUM (
    'SHIPMENT',
    'BOOKING',
    'PAYMENT',
    'MESSAGE',
    'SYSTEM'
);

ALTER TABLE notifications
ADD COLUMN reference_type reference_type;
