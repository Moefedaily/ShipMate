ALTER TABLE insurance_claims
    ALTER COLUMN claim_status TYPE varchar(50)
    USING claim_status::text;

DROP TYPE IF EXISTS claim_status;