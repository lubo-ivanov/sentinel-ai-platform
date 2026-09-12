ALTER TABLE incidents
    ADD COLUMN fingerprint      VARCHAR(64)       NOT NULL DEFAULT '',
    ADD COLUMN first_seen       TIMESTAMPTZ        NOT NULL DEFAULT now(),
    ADD COLUMN last_seen        TIMESTAMPTZ        NOT NULL DEFAULT now(),
    ADD COLUMN anomaly_count    INTEGER            NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX incidents_open_fingerprint_uidx
    ON incidents (fingerprint)
    WHERE status = 'OPEN';