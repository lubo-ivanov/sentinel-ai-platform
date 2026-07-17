CREATE TABLE raw_signals (
                             id           UUID         PRIMARY KEY,
                             external_id  VARCHAR(128),
                             source       VARCHAR(64)  NOT NULL,
                             occurred_at  TIMESTAMPTZ  NOT NULL,
                             message      TEXT         NOT NULL,
                             hints        JSONB,
                             received_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_raw_signals_source_occurred_at
    ON raw_signals (source, occurred_at DESC);