-- FX Pulse - Initial exchange-rate schema
-- Version: V1

-- Separate raw source data from future analytical tables
CREATE SCHEMA IF NOT EXISTS raw;

-- Store foreign-exchange reference-rate observations
CREATE TABLE raw.fx_rates
(

    provider       VARCHAR(16)     NOT NULL,

    rate_date      DATE            NOT NULL,

    base_currency  CHAR(3)         NOT NULL,

    quote_currency CHAR(3)         NOT NULL,

    rate           NUMERIC(20, 10) NOT NULL,

    ingested_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_fx_rates
        PRIMARY KEY (
                     provider,
                     rate_date,
                     base_currency,
                     quote_currency
            ),

    CONSTRAINT chk_fx_rate_positive
        CHECK (rate > 0),

    CONSTRAINT chk_fx_currency_pair
        CHECK (base_currency <> quote_currency)
);
