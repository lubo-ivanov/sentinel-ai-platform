CREATE TABLE operational_events (
                                    id                        UUID             PRIMARY KEY,
                                    source_signal_id          UUID             NOT NULL REFERENCES raw_signals(id),
                                    source                    VARCHAR(64)      NOT NULL,
                                    timestamp                 TIMESTAMPTZ      NOT NULL,
                                    type                      VARCHAR(64)      NOT NULL,
                                    severity                  VARCHAR(16)      NOT NULL,
                                    classification_method     VARCHAR(16)      NOT NULL,
                                    classification_rule_id    VARCHAR(128),
                                    classification_confidence DOUBLE PRECISION NOT NULL,
                                    payload                   JSONB,
                                    created_at                TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_op_events_source_timestamp ON operational_events (source, timestamp DESC);
CREATE INDEX idx_op_events_type ON operational_events (type);