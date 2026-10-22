
# FX Pulse - API Exploration

## Data Source

API: Frankfurter v2
Provider: European Central Bank (ECB)
Base URL: https://api.frankfurter.dev/v2

The project uses daily reference exchange rates,
not real-time trading prices.

## Latest Rates

Endpoint:
GET /v2/providers/ecb/rates?quotes=usd,gbp

## Historical Rates

Endpoint:
GET /v2/providers/ecb/rates?from=2025-01-01&to=2025-01-10&quotes=usd

Historical sample saved to: docs/sample.json

## JSON Response Fields

- date: observation date
- base: base currency code
- quote: target currency code
- rate: numeric exchange rate

## Observations

- API v2 returns an array of rate records.
- Filtering by provider keeps the source consistent.
- Historical results may exclude weekends and holidays.
- Returned observation dates must be preserved.


## Engineering Decision

FX Pulse will use the ECB provider-specific
Frankfurter v2 endpoint for reference-rate ingestion.
