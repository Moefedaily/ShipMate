ALTER TABLE driver_profiles
ADD COLUMN last_latitude NUMERIC(9,6),
ADD COLUMN last_longitude NUMERIC(9,6),
ADD COLUMN last_location_updated_at TIMESTAMPTZ;
