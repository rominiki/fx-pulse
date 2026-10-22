
# FX Pulse - Data Contract

## 1. Purpose

This document defines the structure, validation rules,
and uniqueness requirements for exchange-rate observations.

The initial data source is the Frankfurter API v2,
using the European Central Bank (ECB) provider.

## 2. Source

API:
https://api.frankfurter.dev/v2/providers/ecb/rates

Data frequency: Daily reference rates.

Rates are not live trading prices.

The observation date must come from the API response,
not from the time at which our application downloads it.

## 3. Raw Data Model

Table: raw.fx_rates

| Column | PostgreSQL Type | Required | Description |
|---|---|---|---|
| provider | TEXT | Yes | Rate publisher, initially ECB |
| rate_date | DATE | Yes | Source observation date |
| base_currency | CHAR(3) | Yes | Base currency, e.g. EUR |
| quote_currency | CHAR(3) | Yes | Quote currency, e.g. USD |
| rate | NUMERIC(20,10) | Yes | Exchange-rate value |

## 4. Mapping

| Frankfurter API | Database |
|---|---|
| API provider route | provider = ECB |
| date | rate_date |
| base | base_currency |
| quote | quote_currency |
| rate | rate |

## 5. Primary Key

(provider, rate_date, base_currency, quote_currency)

This combination identifies one observation.

For example:

(ECB, 2025-01-03, EUR, USD)

must be unique.

A different quote currency or observation date
represents a different observation.

## 6. Validation Rules

1. No required field may be null.
2. Rate must be greater than zero.
3. Currency codes must be valid three-letter
   uppercase currency codes.
4. A date must be parseable as YYYY-MM-DD.
5. The same composite key must not be inserted twice.
6. Malformed source records must be rejected or
   quarantined, not silently stored as valid data.

## 7. Duplicate and Conflict Policy

Identical observation, identical primary key:
Do not insert a duplicate.

Same primary key, different rate:
Flag as a conflict or possible source correction.
Do not silently overwrite the stored observation.

The ingestion implementation will enforce this policy
in a later development week.

## 8. Missing Dates

The ECB does not publish a new reference rate
on every calendar day.

A missing weekend or holiday date does not
automatically indicate a pipeline error.

Do not generate artificial source observations
to fill these dates.

## 9. Example Record

{
"provider": "ECB",
"rate_date": "2025-01-03",
"base_currency": "EUR",
"quote_currency": "USD",
"rate": 1.0298
}

Example values are illustrative.

## 10. Planned PostgreSQL Schema

CREATE SCHEMA IF NOT EXISTS raw;

CREATE TABLE raw.fx_rates (
provider TEXT NOT NULL,
rate_date DATE NOT NULL,
base_currency CHAR(3) NOT NULL,
quote_currency CHAR(3) NOT NULL,
rate NUMERIC(20,10) NOT NULL
CHECK (rate > 0),
PRIMARY KEY (
provider,
rate_date,
base_currency,
quote_currency
)
);

This schema is a design draft.
The actual database and migrations will be
implemented later.
