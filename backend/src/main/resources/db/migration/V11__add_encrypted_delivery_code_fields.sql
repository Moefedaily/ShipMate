-- Add encrypted delivery code fields (production-grade storage)

ALTER TABLE shipments
    ADD COLUMN delivery_code_enc TEXT;

ALTER TABLE shipments
    ADD COLUMN delivery_code_iv TEXT;
