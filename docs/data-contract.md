# FX Pulse - Data Contract

## Purpose

This document defines the structure, validation rules,
and uniqueness requirements for exchange-rate observations.

The initial data source is the Frankfurter API v2,
using the European Central Bank (ECB) provider.

## Source

API:
https://api.frankfurter.dev/v2/providers/ecb/rates

Data frequency: Published business-day reference rates (not guaranteed on every calendar date).

Rates are not live trading prices.

The observation date must come from the API response,
not from the time at which our application downloads it.

## Raw Data Model

Table: raw.fx_rates

| Column | PostgreSQL Type | Required | Description |
|---|---|---|---|
| provider | VARCHAR(16) | Yes | Rate publisher, initially ECB |
| rate_date | DATE | Yes | Source observation date |
| base_currency | CHAR(3) | Yes | Base currency |
| quote_currency | CHAR(3) | Yes | Quote currency |
| rate | NUMERIC(20,10) | Yes | Positive exchange rate |
| ingested_at | TIMESTAMPTZ | Yes | Database-generated ingestion timestamp |

## Mapping

| Frankfurter API | Database |
|---|---|
| API provider route | provider = ECB |
| date | rate_date |
| base | base_currency |
| quote | quote_currency |
| rate | rate |

## Primary Key

(provider, rate_date, base_currency, quote_currency)

This combination identifies one observation.

For example:

(ECB, 2025-01-03, EUR, USD)

must be unique.

A different quote currency or observation date
represents a different observation.

## Validation Rules

1. No required field may be null.
2. Rate must be greater than zero.
3. Currency codes must match three uppercase letters (`[A-Z]{3}`); the ingestion service supports EUR as base and rejects EUR as quote. This is a format check, not a check against an ISO currency registry.
4. A date must be parseable as YYYY-MM-DD.
5. The same composite key must not be inserted twice.
6. Malformed source records must be rejected or
   quarantined, not silently stored as valid data.

## Duplicate and Conflict Policy

Identical observation, identical primary key:
Do not insert a duplicate.

Same primary key, different rate:
Flag as a conflict or possible source correction.
Do not silently overwrite the stored observation.

The ingestion service enforces this policy using conflict-safe PostgreSQL inserts and rate comparison.

## Missing Dates

The ECB does not publish a new reference rate
on every calendar day.

A missing weekend or holiday date does not
automatically indicate a pipeline error.

Do not generate artificial source observations
to fill these dates.

## Example Record

{
"provider": "ECB",
"rate_date": "2025-01-03",
"base_currency": "EUR",
"quote_currency": "USD",
"rate": 1.0298
}

Example values are illustrative.

## Implemented PostgreSQL Schema

The initial schema is managed through Flyway.

Migration:
ingestion-service/src/main/resources/db/migration/V1__create_fx_rates.sql

Flyway is responsible for schema creation and evolution.
Hibernate validates the schema against the Java entity mappings.

Future schema modifications must use new versioned migrations. The applied V1 migration must not be edited.

The V1 migration defines `provider VARCHAR(16)`, a database-generated `ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, the four-column primary key, `rate > 0`, and a constraint prohibiting identical base and quote currencies.

```sql
CREATE SCHEMA IF NOT EXISTS raw;

CREATE TABLE raw.fx_rates (
    provider       VARCHAR(16) NOT NULL,
    rate_date      DATE NOT NULL,
    base_currency  CHAR(3) NOT NULL,
    quote_currency CHAR(3) NOT NULL,
    rate           NUMERIC(20,10) NOT NULL,
    ingested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_fx_rates PRIMARY KEY
        (provider, rate_date, base_currency, quote_currency),
    CONSTRAINT chk_fx_rate_positive CHECK (rate > 0),
    CONSTRAINT chk_fx_currency_pair CHECK (base_currency <> quote_currency)
);
```

- Source endpoint: `https://api.frankfurter.dev/v2/providers/ecb/rates` (Frankfurter API v2, ECB-only).
- Incoming v2 record: `date: LocalDate`, `base: String`, `quote: String`, `rate: BigDecimal`.
- Persist only `EUR` to requested uppercase three-letter quote; quote must not equal EUR.
- Record date must be within the requested range; a valid observation outside range is ignored and counted.
- Reject null/nonpositive rates, mismatched currency codes, and rates not exactly representable by `NUMERIC(20,10)`.
- Composite key: `(provider, rate_date, base_currency, quote_currency)`.
- New row: inserted. Same key and equal decimal value: skipped. Same key, changed value: conflict / no silent overwrite.
- Each monthly window writes in a transaction. An invalid or conflicting batch is rolled back; earlier committed windows remain so reruns can resume.
- Real Frankfurter/ECB data may omit weekends or publication holidays; no forward filling or invented daily rows.
- Single manual backfill request is limited to 366 days inclusive and partitioned by calendar month.
