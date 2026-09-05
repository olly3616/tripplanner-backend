-- V5: shared expenses and their splits.
-- Money is stored as integer minor units. amount_minor / split.amount_minor are in
-- the expense currency; base_amount_minor is in the trip's base currency.
-- exchange_rate is defined as (base minor per expense minor); base is snapshotted at
-- creation so later rate changes never alter past settlements.

CREATE TABLE expenses
(
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    trip_id           BIGINT        NOT NULL,
    payer_id          BIGINT        NOT NULL,
    title             VARCHAR(150)  NOT NULL,
    amount_minor      BIGINT        NOT NULL,
    currency          CHAR(3)       NOT NULL,
    exchange_rate     DECIMAL(20, 8) NOT NULL,
    base_amount_minor BIGINT        NOT NULL,
    category          VARCHAR(50)   NULL,
    split_method      VARCHAR(20)   NOT NULL,
    spent_on          DATE          NOT NULL,
    receipt_url       VARCHAR(512)  NULL,
    note              VARCHAR(1000) NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_expenses_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_payer FOREIGN KEY (payer_id) REFERENCES users (id),
    CONSTRAINT ck_expenses_amount CHECK (amount_minor > 0),
    CONSTRAINT ck_expenses_split_method CHECK (split_method IN ('EQUAL', 'RATIO', 'EXACT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_expenses_trip ON expenses (trip_id);

CREATE TABLE expense_splits
(
    id           BIGINT NOT NULL AUTO_INCREMENT,
    expense_id   BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    amount_minor BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_splits_expense FOREIGN KEY (expense_id) REFERENCES expenses (id) ON DELETE CASCADE,
    CONSTRAINT fk_splits_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_splits_expense_user UNIQUE (expense_id, user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_splits_expense ON expense_splits (expense_id);
