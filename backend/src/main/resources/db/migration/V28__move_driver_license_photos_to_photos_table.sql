ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS driver_profile_id UUID REFERENCES driver_profiles(id) ON DELETE CASCADE;

UPDATE photos p
SET driver_profile_id = dp.id
FROM driver_profiles dp
WHERE dp.license_photo_id = p.id
  AND p.driver_profile_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_photos_driver_profile_id ON photos(driver_profile_id);

ALTER TABLE driver_profiles DROP CONSTRAINT IF EXISTS fk_driver_profile_license_photo;
ALTER TABLE driver_profiles DROP COLUMN IF EXISTS license_photo_id;
