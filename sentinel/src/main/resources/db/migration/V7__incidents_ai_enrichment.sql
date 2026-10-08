ALTER TABLE incidents
    ADD COLUMN ai_summary        TEXT,
    ADD COLUMN ai_likely_cause   TEXT,
    ADD COLUMN ai_generated_at   TIMESTAMP WITH TIME ZONE,
    ADD COLUMN ai_summary_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';