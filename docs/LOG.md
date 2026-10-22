
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

## 1-5

### Completed

- Created ADR 0001 explaining the ECB data-source decision
- Updated README to distinguish completed and planned work
- Documented commands to reproduce the Python prototype
- Reviewed the data contract and architecture
- Verified the analytics script
- Prepared the v0.1-design release

### One Lesson Learned

A successful data pipeline is not just about downloading
records. It also needs traceable sources, explicit data
contracts, sensible validation and reproducible outputs.

### One Unresolved Question

How should FX Pulse handle a source correction when an
observation already exists with the same provider, date
and currency pair but a different rate?

This will be addressed during ingestion design.

### Next

- Create the Java 21 Spring Boot application
- Add the Maven Wrapper
- Run PostgreSQL through Docker Compose
- Add versioned database migrations
- Implement the first database-backed service


## 2-1

### Goal
Create the initial Java 21 Spring Boot service.

### Completed
- Generated the Spring Boot Maven project
- Added Spring Web, JPA, Validation,
  PostgreSQL Driver, Flyway and Actuator
- Created the initial package structure
- Implemented GET /api/v1/ping
- Verified HTTP 200 and {"status":"ok"}
- Added a MockMvc controller test
- Executed Maven tests successfully

### Architecture Notes
- The application is located in ingestion-service/.
- The endpoint uses Spring MVC.
- Database auto-configuration is temporarily disabled.
- PostgreSQL will be configured later.

### Next
- Start PostgreSQL using Docker Compose.
- Externalize database credentials.
- Verify database connectivity.


## 2-2

### Goal

Run PostgreSQL locally using Docker Compose.

### Completed

- Created PostgreSQL Docker Compose service
- Added named volume for persistent storage
- Externalized database credentials
- Created .env.example
- Verified .env is excluded from Git
- Started the PostgreSQL container
- Verified the container healthcheck
- Executed SELECT version()
- Documented start, stop and restart commands

### Next

- Create the raw database schema
- Add Flyway V1 migration
- Create the fx_rates table
- Test schema initialization
