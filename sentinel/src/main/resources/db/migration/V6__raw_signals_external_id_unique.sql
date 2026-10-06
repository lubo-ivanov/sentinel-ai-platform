ALTER TABLE raw_signals
    ALTER COLUMN external_id SET NOT NULL,
    ADD CONSTRAINT uq_raw_signals_external_id UNIQUE (external_id);