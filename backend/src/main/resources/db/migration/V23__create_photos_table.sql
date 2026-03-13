CREATE TABLE photos (
    id UUID PRIMARY KEY,
    url TEXT NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    photo_type VARCHAR(50) NOT NULL, -- e.g., 'AVATAR', 'SHIPMENT_PACKAGE', 'CLAIM_PROOF', 'MESSAGE_IMAGE'
    
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    shipment_id UUID REFERENCES shipments(id) ON DELETE CASCADE,
    insurance_claim_id UUID REFERENCES insurance_claims(id) ON DELETE CASCADE,
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_photos_user_id ON photos(user_id);
CREATE INDEX idx_photos_shipment_id ON photos(shipment_id);
CREATE INDEX idx_photos_insurance_claim_id ON photos(insurance_claim_id);
CREATE INDEX idx_photos_message_id ON photos(message_id);

-- 1. Migrate Avatars from users table
INSERT INTO photos (id, url, public_id, photo_type, user_id, created_at)
SELECT 
    gen_random_uuid(), 
    avatar_url, 
    COALESCE(avatar_public_id, 'legacy-avatar-' || id), 
    'AVATAR', 
    id, 
    created_at
FROM users 
WHERE avatar_url IS NOT NULL;

-- 2. Migrate Shipment Photos (JSONB array to rows)
INSERT INTO photos (id, url, public_id, photo_type, shipment_id, created_at)
SELECT 
    gen_random_uuid(), 
    photo_url, 
    'legacy-shipment-' || id || '-' || (row_number() OVER (PARTITION BY id)), 
    'SHIPMENT_PACKAGE', 
    id, 
    created_at
FROM shipments, jsonb_array_elements_text(photos) AS photo_url
WHERE photos IS NOT NULL;

-- 3. Migrate Insurance Claim Photos (Legacy might be TEXT or JSON, V6 migration said TEXT, but model said JSONB)
-- Based on model analysis, it's used as a List<String> with @JdbcTypeCode(SqlTypes.JSON)
INSERT INTO photos (id, url, public_id, photo_type, insurance_claim_id, created_at)
SELECT 
    gen_random_uuid(), 
    photo_url, 
    'legacy-claim-' || id || '-' || (row_number() OVER (PARTITION BY id)), 
    'CLAIM_PROOF', 
    id, 
    created_at
FROM insurance_claims, jsonb_array_elements_text(photos::jsonb) AS photo_url
WHERE photos IS NOT NULL;
