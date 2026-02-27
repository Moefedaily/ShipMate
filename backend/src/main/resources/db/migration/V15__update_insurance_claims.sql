CREATE TYPE claim_reason AS ENUM (
    'LOST',
    'DAMAGED',
    'OTHER'
);

ALTER TABLE insurance_claims
ADD COLUMN declared_value_snapshot NUMERIC(10,2) NOT NULL,
ADD COLUMN coverage_amount NUMERIC(10,2) NOT NULL,
ADD COLUMN deductible_rate NUMERIC(5,4) NOT NULL,
ADD COLUMN compensation_amount NUMERIC(10,2) NOT NULL,
ADD COLUMN claim_reason claim_reason NOT NULL;

ALTER TABLE insurance_claims
DROP COLUMN IF EXISTS claim_amount;

ALTER TABLE insurance_claims
ALTER COLUMN photos TYPE jsonb USING photos::jsonb;

ALTER TABLE insurance_claims
ADD CONSTRAINT uk_insurance_claim_shipment UNIQUE (shipment_id);