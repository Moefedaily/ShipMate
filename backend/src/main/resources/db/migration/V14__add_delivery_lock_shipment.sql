ALTER TABLE shipments
ADD COLUMN delivery_locked BOOLEAN NOT NULL DEFAULT false;
