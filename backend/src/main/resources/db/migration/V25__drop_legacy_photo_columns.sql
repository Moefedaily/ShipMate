-- V25__drop_legacy_photo_columns.sql

-- 1. Drop from users
ALTER TABLE users DROP COLUMN IF EXISTS avatar_url;
ALTER TABLE users DROP COLUMN IF EXISTS avatar_public_id;

-- 2. Drop from shipments
ALTER TABLE shipments DROP COLUMN IF EXISTS photos;

-- 3. Drop from insurance_claims
ALTER TABLE insurance_claims DROP COLUMN IF EXISTS photos;
