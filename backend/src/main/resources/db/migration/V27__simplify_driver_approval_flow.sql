-- Remove separate license approval fields from driver_profiles
ALTER TABLE driver_profiles DROP COLUMN IF EXISTS license_status;
ALTER TABLE driver_profiles DROP COLUMN IF EXISTS license_rejection_reason;

-- Synchronize vehicle status with driver status
-- If a driver is already APPROVED, their current vehicles should also be APPROVED and one should be active
UPDATE vehicles v
SET status = 'APPROVED', 
    is_active = TRUE
FROM driver_profiles dp
WHERE v.driver_profile_id = dp.id 
  AND dp.status = 'APPROVED'
  -- Ensure only one is active per driver if they had multiple (though unlikely after V26)
  AND v.id = (
      SELECT id FROM vehicles 
      WHERE driver_profile_id = dp.id 
      ORDER BY created_at ASC 
      LIMIT 1
  );

-- For any other vehicle belonging to an approved driver, ensure it is at least APPROVED
UPDATE vehicles v
SET status = 'APPROVED'
FROM driver_profiles dp
WHERE v.driver_profile_id = dp.id 
  AND dp.status = 'APPROVED';
