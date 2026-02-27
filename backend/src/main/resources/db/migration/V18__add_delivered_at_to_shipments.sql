ALTER TABLE shipments
ADD COLUMN delivered_at TIMESTAMPTZ;

CREATE INDEX idx_shipments_delivered_at ON shipments(delivered_at);