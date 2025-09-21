-- V1__Initial_schema.sql

CREATE TABLE transactions
(
    id                     UUID         NOT NULL,
    owner_id               VARCHAR(255) NOT NULL,
    source_account_id      UUID         NOT NULL,
    destination_account_id UUID         NOT NULL,
    amount                 DECIMAL(19, 4) NOT NULL,
    currency               VARCHAR(3)   NOT NULL,
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version                BIGINT, -- Optimistic locking version field
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

CREATE TABLE transaction_status_history
(
    id             UUID         NOT NULL,
    transaction_id UUID         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    reason         VARCHAR(255),
    timestamp      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_transaction_status_history PRIMARY KEY (id)
);

ALTER TABLE transaction_status_history
    ADD CONSTRAINT FK_TRANSACTION_STATUS_HISTORY_ON_TRANSACTION FOREIGN KEY (transaction_id) REFERENCES transactions (id);

CREATE INDEX idx_transaction_id_timestamp_desc ON transaction_status_history (transaction_id, timestamp DESC);

-- Add idempotency keys table
CREATE TABLE idempotency_keys
(
    key             UUID         NOT NULL,
    response_status INT          NOT NULL,
    response_body   TEXT         NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_idempotency_keys PRIMARY KEY (key)
);
