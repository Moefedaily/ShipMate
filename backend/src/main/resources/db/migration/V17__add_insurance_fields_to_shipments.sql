ALTER TABLE shipments
    RENAME COLUMN extra_insurance_fee TO insurance_fee;

ALTER TABLE shipments
    ADD COLUMN insurance_selected BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE shipments
    ADD COLUMN declared_value NUMERIC(10,2);

ALTER TABLE shipments
    ADD COLUMN insurance_coverage_amount NUMERIC(10,2);

ALTER TABLE shipments
    ADD COLUMN insurance_deductible_rate NUMERIC(5,4);