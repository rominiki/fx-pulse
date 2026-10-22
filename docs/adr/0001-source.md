
# ADR 0001 - Use ECB Reference Rates via Frankfurter v2

**Status:** Accepted

## Context

FX Pulse needs a publicly accessible source of
historical and daily foreign-exchange rates.

The source should be:
- Accessible without an API key
- Suitable for historical analysis
- Clearly attributable to a financial institution
- Easy to integrate using HTTP and JSON

Frankfurter v2 offers exchange rates from multiple
providers, including the European Central Bank (ECB).

## Decision

FX Pulse will initially use the provider-specific
Frankfurter v2 endpoint:

https://api.frankfurter.dev/v2/providers/ecb/rates

We will store:
- Source provider: ECB
- Source observation date
- Base currency
- Quote currency
- Exchange rate

The initial implementation will focus on EUR-based
currency pairs.

## Reasons

### 1. Consistent data source

Pinning the ECB provider avoids silently mixing
rates from different publishers.

### 2. Historical availability

Historical records allow us to calculate returns,
moving averages and volatility metrics.

### 3. Simple integration

Frankfurter returns structured JSON over HTTPS.
The public API does not require an API key.

### 4. Reproducible development

We can use fixed historical date ranges in
demonstrations and automated tests.

## Consequences and Limitations

- ECB reference rates are not live trading quotes.
- Rates may not be published on weekends or holidays.
- The latest available observation date may differ
  from the date on which our application runs.
- External API availability is outside our control.
- The API may include a preceding observation around
  a requested date boundary; reporting logic should
  verify its date range.
- We must retain source dates rather than generating
  artificial observations.

## Alternatives Considered

### Blended Frankfurter rates

Rejected for the initial version because the
underlying publisher can vary between currency pairs.

### Live trading-rate providers

Not selected because the project's initial goal
is historical data engineering, not live execution,
trading or price discovery.

## Future Reconsideration

Additional providers may be introduced later,
provided we preserve provider identity and define
how conflicting observations are handled.
