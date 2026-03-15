
ALTER TABLE driver_profiles ADD COLUMN license_photo_id UUID;
ALTER TABLE driver_profiles ADD COLUMN license_expiry DATE;
ALTER TABLE driver_profiles ADD COLUMN license_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE driver_profiles ADD COLUMN license_rejection_reason TEXT;

ALTER TABLE driver_profiles ALTER COLUMN license_number DROP NOT NULL;

ALTER TABLE driver_profiles 
    ADD CONSTRAINT fk_driver_profile_license_photo 
    FOREIGN KEY (license_photo_id) REFERENCES photos(id);

UPDATE driver_profiles 
SET license_status = 'APPROVED' 
WHERE status = 'APPROVED';

CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_profile_id UUID NOT NULL REFERENCES driver_profiles(id) ON DELETE CASCADE,
    vehicle_type VARCHAR(50) NOT NULL,
    max_weight_capacity NUMERIC(6,2) NOT NULL,
    plate_number VARCHAR(20),
    insurance_expiry DATE,
    vehicle_description TEXT,
    rejection_reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vehicles_driver_profile_id ON vehicles(driver_profile_id);
CREATE INDEX idx_vehicles_status ON vehicles(status);


INSERT INTO vehicles (
    id, 
    driver_profile_id, 
    vehicle_type, 
    max_weight_capacity, 
    vehicle_description, 
    status, 
    is_active, 
    created_at, 
    updated_at
)
SELECT 
    gen_random_uuid(), 
    id, 
    vehicle_type, 
    max_weight_capacity, 
    vehicle_description, 
    CASE WHEN status = 'APPROVED' THEN 'APPROVED' ELSE 'PENDING' END,
    CASE WHEN status = 'APPROVED' THEN TRUE ELSE FALSE END,
    now(), 
    now()
FROM driver_profiles
WHERE vehicle_type IS NOT NULL;

ALTER TABLE driver_profiles DROP COLUMN vehicle_type;
ALTER TABLE driver_profiles DROP COLUMN max_weight_capacity;
ALTER TABLE driver_profiles DROP COLUMN vehicle_description;
