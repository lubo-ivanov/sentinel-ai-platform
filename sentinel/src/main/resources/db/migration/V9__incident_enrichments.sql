CREATE TABLE incident_enrichments
(
    id                UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    incident_id       UUID        NOT NULL REFERENCES incidents (id),
    ai_summary        TEXT,
    ai_likely_cause   TEXT,
    remediation_steps TEXT,
    ai_summary_status VARCHAR(16) NOT NULL,
    generated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_enrichments_incident_id UNIQUE (incident_id)
);

ALTER TABLE incidents
    DROP COLUMN ai_summary,
    DROP COLUMN ai_likely_cause,
    DROP COLUMN ai_generated_at,
    DROP COLUMN ai_summary_status,
    DROP COLUMN remediation_steps;