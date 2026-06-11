CREATE TABLE incidents
(
    id         UUID PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    severity   VARCHAR(16)  NOT NULL,
    status     VARCHAR(16)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version    BIGINT       NOT NULL DEFAULT 0
);
