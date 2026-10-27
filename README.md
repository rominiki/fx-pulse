# FX Pulse

**Java 21 · Spring Boot 4.1.1 · PostgreSQL 16 · Docker Compose · GitHub Actions**

FX Pulse is a portfolio financial-data service for historical and daily **European Central Bank (ECB) reference exchange rates** fetched through [Frankfurter API v2](https://frankfurter.dev/).

It validates source observations, stores them in PostgreSQL, supports repeatable historical backfills, and exposes a read-only REST API and basic statistics.

**These are reference rates, not executable trading quotes or a real-time market feed.**

## Features

### Exchange Rate Ingestion

- ECB-specific Frankfurter v2 HTTP client with bounded retries for transient failures and structured upstream errors.
- Validated EUR-to-quote exchange rates using precise `BigDecimal` values and preserved source publication dates.
- Manual single-date ingestion and historical backfills covering up to 366 inclusive days.
- Historical backfills processed in calendar-month windows.
- Idempotent PostgreSQL persistence using atomic unique-key inserts.
- Identical observations are skipped on repeated ingestion; conflicting values are reported without silent overwrites.

### REST API and Statistics

- Paginated and sorted historical exchange-rate queries.
- Filtering by date range and quote currency.
- Basic statistics including observation count, minimum, maximum, average, and latest exchange rates.
- Structured validation and upstream error responses.
- Interactive Swagger UI and OpenAPI documentation.

### Infrastructure and Testing

- Java 21 and Spring Boot 4.1.1 backend.
- PostgreSQL 16 with Flyway database migrations.
- Docker Compose deployment for the application and database.
- JUnit, MockMvc, Mockito, PostgreSQL integration tests, and Testcontainers.
- GitHub Actions CI for automated building and testing.
- End-to-end PowerShell smoke test (`scripts/smoke-test.ps1`).

**Verification:** The Java MVP has passed local automated tests, PostgreSQL integration tests, Docker Compose startup, historical ingestion and idempotency checks, exchange-rate and statistics queries, GitHub Actions CI, and the end-to-end PowerShell smoke test.

## Architecture

```text
Frankfurter API v2 (ECB reference rates)
                |
                v
Spring RestClient (HTTP + retry)
                |
                v
Ingestion and validation services
                |
                v
PostgreSQL raw.fx_rates
(Flyway migrations + compound primary key)
                |
                v
Spring REST API
(Historical rates + basic statistics)
```

Python/pandas exploration is available in `analytics/`.

Apache Airflow scheduling, production analytical transformations, Snowflake comparison, and dashboard development are planned future capabilities and are not included in this Java MVP.

## Quick Start — Docker

### Requirements

- Docker Desktop with Docker Compose
- Git

### 1. Clone the repository

```powershell
git clone https://github.com/rominiki/fx-pulse.git
cd fx-pulse
```

### 2. Configure environment variables

Copy the example environment file:

```powershell
Copy-Item .env.example .env
```

Set `DB_USER` and `DB_PASSWORD` in `.env`.

Keep the database credentials consistent with any existing PostgreSQL data volume.

`FX_API_URL` can remain configured to use the ECB-specific Frankfurter v2 endpoint.

**Never commit `.env` or real database credentials to Git.**

### 3. Start the application

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

Docker Compose starts:

- `db` — PostgreSQL 16 with persistent storage and health checks.
- `ingestion-service` — Spring Boot application, available on port `8081`.

Stop any locally running IntelliJ application already using port `8081`.

### 4. Verify application health

```powershell
curl.exe http://localhost:8081/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/ingestion` | Ingest exchange rates for a specified date |
| `POST` | `/api/v1/ingestion/backfill` | Ingest historical rates over a date interval |
| `GET` | `/api/v1/rates` | Retrieve paginated historical exchange rates |
| `GET` | `/api/v1/stats/basic` | Retrieve basic exchange-rate statistics |
| `GET` | `/actuator/health` | Application health |
| `GET` | `/v3/api-docs` | OpenAPI specification |

### Example requests

**Ingest a single date**

```powershell
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion?date=2025-01-02&quote=USD"
```

**Run a historical backfill**

```powershell
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion/backfill?from=2025-01-01&to=2025-01-31&quote=USD"
```

**Query exchange rates**

```powershell
curl.exe "http://localhost:8081/api/v1/rates?from=2025-01-01&to=2025-01-31&quote=USD&page=0&size=20"
```

**Query basic statistics**

```powershell
curl.exe "http://localhost:8081/api/v1/stats/basic?from=2025-01-01&to=2025-01-31&quote=USD"
```

### API documentation

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs
- Health check: http://localhost:8081/actuator/health

The rates endpoint returns paginated results with `content`, `page`, `size`, `totalElements`, and `totalPages`.

The statistics endpoint returns observation counts and date/rate summaries. If no observations exist for the requested interval, it returns zero observations and null statistics.

Weekends and ECB holidays are not expected to have published reference rates.

## Testing and Verification

### Automated tests

Requirements: Java 21, appropriate PostgreSQL test configuration, and Docker for Testcontainers.

From the `ingestion-service` directory:

```powershell
.\mvnw.cmd clean verify
```

On Ubuntu/Linux:

```bash
./mvnw -B clean verify
```

The test suite includes unit tests, API tests, database integration tests, and Testcontainers-based tests.

The GitHub Actions workflow automatically executes the backend verification process on pushes and pull requests targeting `main`.

Some integration tests use the separate `fxpulse_migration_test` database. The GitHub Actions workflow provisions PostgreSQL for these tests.

### End-to-end smoke test

With the application running, execute from the repository root:

```powershell
.\scripts\smoke-test.ps1
```

The smoke test verifies:

1. Application health.
2. Historical exchange-rate ingestion.
3. Idempotency through a repeated backfill with zero new inserts.
4. Retrieval of stored historical exchange rates.
5. Consistency of statistics and observation counts.
6. Availability of the documented OpenAPI endpoints.

### Stop the application

```powershell
docker compose down
```

PostgreSQL data remains stored in the Docker volume.

**Warning:** `docker compose down -v` removes the database volume and its persisted data.

## Persistence Contract

Exchange rates are stored in `raw.fx_rates`.

Each observation includes:

- Provider identifier.
- Source publication date.
- Base currency.
- Quote currency.
- Precise decimal exchange rate.
- Ingestion timestamp.

The composite primary key is:

`(provider, rate_date, base_currency, quote_currency)`

Repeated identical observations do not create duplicates. Conflicting rates for an existing key are rejected rather than silently overwritten.

See the [Data Contract](docs/data-contract.md) for schema details and ingestion behavior.

## Project Documentation

- [Setup and Troubleshooting](docs/setup.md)
- [Data Contract and Conflict Policy](docs/data-contract.md)
- [Architecture](docs/architecture.md)
- [Engineering Log](docs/LOG.md)
- [Release Notes](docs/release-notes.md)
- [ECB Data Source Decision](docs/adr/0001-source.md)

## Releases

### Current Release - v0.4 Java Backend MVP

**[FX Pulse v0.4 - Java Backend MVP](https://github.com/rominiki/fx-pulse/releases/tag/v0.4-java-mvp)**

This release delivers the functional Java backend with ECB ingestion, PostgreSQL persistence, historical backfills, REST APIs, statistics, Docker deployment, automated tests, and GitHub Actions CI.

The release identifier `v0.4-java-mvp` is independent of the Spring Boot and PostgreSQL versions.

### Previous Release

`v0.1-design` - Initial project architecture, data contract, source selection, and Python exploration.

## Roadmap

The following capabilities are planned for subsequent development:

- Financial analytics using SQL and Python.
- Apache Airflow orchestration for scheduled ingestion.
- Data quality monitoring and validation.
- Snowflake integration and analytical comparisons.
- Visualization and interactive dashboards.

## Known Limitations

- ECB reference rates are not live market prices or executable trading quotes.
- Continuous scheduling is not yet included; ingestion is manually triggered through REST endpoints.
- Statistics are intentionally limited to basic descriptive measures.
- The current deployment is designed for local development, automated testing, and portfolio demonstration rather than production hosting.

## License

Licensed under the MIT License. See [LICENSE](LICENSE).
