ALTER TABLE notifications
ADD COLUMN reference_id UUID;

CREATE INDEX idx_notifications_reference_id
ON notifications(reference_id);
