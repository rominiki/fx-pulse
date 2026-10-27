# FX Pulse - FX reference rate data contract

## Provider and meaning

Source: Frankfurter API v2, **ECB provider** (`https://api.frankfurter.dev/v2/providers/ecb/rates`). These observations are ECB reference rates, not executable or intraday trading quotes. Preserve the actual `date` returned by the provider; no invented observations for weekends or publication holidays.

## Payload and validation

Incoming JSON is a flat array of `{date, base, quote, rate}` records. `date` maps to Java `LocalDate`, `rate` to `BigDecimal`. Current ingestion supports EUR as base and an uppercase three-letter requested quote other than EUR. Source records must have matching currencies, positive, nonnull rates and valid publication dates. Records outside the requested date range are ignored and counted. A non-representable rate for `NUMERIC(20,10)` is rejected, not rounded silently.

## Stored schema

The authoritative DDL is the applied Flyway migration `ingestion-service/src/main/resources/db/migration/V1__create_fx_rates.sql`. Do **not** edit an already-applied migration; use V2+ for later schema changes.

| Column | PostgreSQL type | Meaning |
|---|---|---|
| `provider` | `VARCHAR(16)` | `ECB` initially |
| `rate_date` | `DATE` | Source publication date |
| `base_currency` | `CHAR(3)` | Base code |
| `quote_currency` | `CHAR(3)` | Quote code |
| `rate` | `NUMERIC(20,10)` | Positive exact decimal |
| `ingested_at` | `TIMESTAMPTZ` | Database ingestion timestamp |

Primary key: `(provider, rate_date, base_currency, quote_currency)`. The migration also constrains positive rates and different base/quote currencies. This document intentionally avoids a second hand-maintained DDL definition so it cannot drift from Flyway.

## Idempotency, conflicts, recovery

New key: insert. Existing key with equal decimal value: skip duplicate. Existing key with different rate: raise conflict; never silently overwrite source corrections. PostgreSQL `INSERT ... ON CONFLICT DO NOTHING` protects uniqueness under races, with explicit existing-value comparison. A conflicting/invalid ingestion batch rolls back its transactional writes.

A manual historical request is bounded to 366 inclusive calendar days and split by calendar-month windows. Completed windows stay committed if a later window fails; the next run safely skips already inserted identical observations.

## API response and error behaviors

- `POST /api/v1/ingestion?date=YYYY-MM-DD&quote=USD`
- `POST /api/v1/ingestion/backfill?from=YYYY-MM-DD&to=YYYY-MM-DD&quote=USD`
- `GET /api/v1/rates?from=YYYY-MM-DD&to=YYYY-MM-DD&quote=USD&page=0&size=20`
- `GET /api/v1/stats/basic?from=YYYY-MM-DD&to=YYYY-MM-DD&quote=USD`

Ingestion reports inserted and skipped counts. Invalid request: HTTP 400. Conflicting stored value: HTTP 409. Invalid/permanent upstream response: HTTP 502. Transient upstream unavailability after bounded retry: HTTP 503. An interval with no stored observations is not an error and returns empty rate records or null summary statistics.
