
# FX Pulse Development Log

## 1-1

### Completed
- Prepared the project folder structure
- Reviewed README and repository configuration
- Created GitHub Project Board
- Created weekly development Issues
- Checked local development tools

### Next
- Explore Frankfurter API v2
- Inspect ECB exchange-rate JSON responses
- Save example exchange-rate data

## 1-2

### Completed
- Explored Frankfurter API v2
- Requested latest EUR/USD and EUR/GBP rates
- Requested historical EUR/USD rates
- Saved historical JSON to docs/sample.json
- Examined response fields and HTTP errors
- Documented API behavior

### Learned
- API v2 uses a flat JSON array.
- ECB data is reference data, not live quotes.
- Missing calendar dates do not necessarily mean missing data.

### Next
- Define the FX rate data model
- Create docs/data-contract.md
- Draft the architecture diagram


## 1-3

### Completed
- Designed the exchange-rate data model
- Defined the composite primary key
- Documented validation and duplicate rules
- Created docs/data-contract.md
- Created docs/architecture.md
- Documented the PostgreSQL design decision

### Learned
- Idempotency prevents duplicate ingestion.
- NUMERIC is preferable to approximate floating-point
  storage for exact decimal rate values.
- Source observation dates must be preserved.

### Next
- Build the first Python/pandas analytics prototype.
- Calculate daily returns and a rolling average.


## 1-4

### Completed
- Created a dedicated Python virtual environment
- Installed requests and pandas
- Created analytics/01_api_exploration.py
- Downloaded historical ECB EUR/USD rates
- Converted API JSON to a pandas DataFrame
- Parsed and sorted observation dates
- Calculated percentage returns
- Calculated five-observation moving averages
- Exported analytics results to CSV
- Executed the script successfully twice

### Learned
- pct_change() calculates relative changes.
- rolling(5).mean() calculates a five-row moving average.
- Initial NaN values are expected and meaningful.
- An observation-based window is not the same as
  a calendar-day window.

### Next
- Review and finalize the Week 1 documentation.
- Add an architecture decision record.
- Prepare the v0.1-design GitHub release.

