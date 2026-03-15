ALTER TABLE driver_profiles
    ADD COLUMN IF NOT EXISTS pending_license_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS pending_license_expiry DATE;
