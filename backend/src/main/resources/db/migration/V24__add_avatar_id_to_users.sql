-- V24__add_avatar_id_to_users.sql

ALTER TABLE users ADD COLUMN avatar_id UUID;

ALTER TABLE users 
ADD CONSTRAINT fk_users_avatar 
FOREIGN KEY (avatar_id) REFERENCES photos(id) ON DELETE SET NULL;

-- Link the photos already migrated in V23 back to the users table
UPDATE users u
SET avatar_id = p.id
FROM photos p
WHERE p.user_id = u.id AND p.photo_type = 'AVATAR';
