ALTER TABLE insurance_claims
    ALTER COLUMN claim_reason TYPE varchar(50)
    USING claim_reason::text;

DROP TYPE claim_reason;