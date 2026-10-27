# FX Pulse - Foreign Exchange Data Platform

> **Status:** Java ingestion and PostgreSQL backend implemented; read-only API, deployment, and automation are in development.  
> **Stack:** Java 21, Spring Boot 4.1.1, PostgreSQL 16, Flyway, Docker Compose, Maven, JUnit, Python/pandas  
> **Source:** [Frankfurter API v2](https://frankfurter.dev/), ECB provider-specific reference rates

FX Pulse is a backend and data engineering portfolio project that retrieves ECB foreign-exchange reference rates, validates and stores observations, supports repeatable historical imports, and provides a foundation for analytics and a public REST API. The source publishes reference rates, **not live tradable quotes**.

## What works

- **Source exploration:** Python/pandas prototype retrieving historical EUR/USD observations, calculating observation-to-observation returns and a five-observation moving average, and exporting CSV.
- **Java backend foundation:** Spring Boot application, Maven Wrapper, `/api/v1/ping`, Actuator health endpoint, externalized configuration.
- **Persistence:** PostgreSQL 16 in Docker Compose, Flyway-managed `raw.fx_rates` schema, JPA entity and composite key, repository tests.
- **Client:** Spring `RestClient` reading the ECB-specific Frankfurter v2 flat JSON array into records, with input checks and bounded retry handling for transient HTTP failures.
- **Ingestion:** validation of input records, `BigDecimal` precision, conflict-safe inserts, duplicate detection and transactional write batches.
- **Manual endpoints:** single-date ingestion and a date-range backfill split into calendar-month windows (366 inclusive days maximum per request).
- **Testing:** mock HTTP tests, controller/service tests, repository tests, and PostgreSQL integration tests. Tests for changes already pushed were reported as passing locally; this does not imply that hosted CI is configured or that a live backfill has been independently verified.

## Planned next

- Read-only, filtered and paginated exchange-rate REST endpoint; basic statistics; OpenAPI.
- Application Docker image and GitHub Actions automated build/tests.
- SQL/Python analytical transformations, Apache Airflow orchestration, data-quality reconciliation, and dashboard.
- Optional Snowflake comparison, dbt, Kafka, Spark, Databricks, and Kubernetes extensions.

## Architecture

```text
Frankfurter API v2 (ECB)
         |
         v
Spring Boot RestClient --> validation --> transactional ingestion
                                              |
                                              v
                                       PostgreSQL raw.fx_rates
                                              |
                              +---------------+---------------+
                              |                               |
                        Read-only API                  SQL/Python analytics
                              |                               |
                          Consumers                         Dashboard
```

Reference observations are stored using their **source publication dates**. No rows are invented for weekends or holidays. Ingesting the same date/pair again must not create duplicate records; a changed value for an existing key is treated as a conflict, not silently overwritten.

More detail: [architecture](docs/architecture.md), [data contract](docs/data-contract.md), [source ADR](docs/adr/0001-source.md).

## HTTP ingestion endpoints

With the application running locally on port 8081:

```powershell
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion?date=2025-01-02&quote=USD"
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion/backfill?from=2025-01-01&to=2025-01-31&quote=USD"
```

Backfill requests are limited to 366 calendar days and processed in month-sized windows. Completed windows remain committed if a later window fails; rerunning safely skips identical records. Responses report received, inserted, duplicate-skipped and outside-range-ignored counts. Expected error responses include 400 (invalid parameters), 409 (rate conflict), 502 (permanent upstream failure) and 503 (temporary upstream failure). Automatic scheduling is not yet implemented.

## Data model

`raw.fx_rates` has columns:

| Column | PostgreSQL type | Meaning |
|---|---|---|
| `provider` | `VARCHAR(16)` | Publisher, currently ECB |
| `rate_date` | `DATE` | Date published by source |
| `base_currency` | `CHAR(3)` | Base currency |
| `quote_currency` | `CHAR(3)` | Quote currency |
| `rate` | `NUMERIC(20,10)` | Positive reference rate |
| `ingested_at` | `TIMESTAMPTZ` | Database-generated insert time |

Primary key: `(provider, rate_date, base_currency, quote_currency)`.

Flyway migration: `ingestion-service/src/main/resources/db/migration/V1__create_fx_rates.sql`. Do not modify this migration after applying it; use new migrations for future schema changes.

## Getting started (Windows PowerShell)

Requirements: Java 21, Docker Desktop with Compose, Git, and Python for the analytics prototype. The Java application includes Maven Wrapper.

```powershell
git clone https://github.com/rominiki/fx-pulse.git
cd fx-pulse
Copy-Item .env.example .env
```

Configure `DB_USER`, `DB_PASSWORD`, `DB_URL`, `FX_API_URL`, and `SERVER_PORT` in local `.env`. Never commit `.env` or secrets. Typical main DB URL: `jdbc:postgresql://localhost:5432/fxpulse`. Existing PostgreSQL volumes retain the original password when `.env` changes.

Start PostgreSQL from the repository root:

```powershell
docker compose up -d db
docker compose ps
```

Load variables and start the Java app from `ingestion-service`:

```powershell
cd ingestion-service
Get-Content ..\.env | Where-Object { $_ -match '^(DB_URL|DB_USER|DB_PASSWORD|FX_API_URL|SERVER_PORT)=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
.\mvnw.cmd spring-boot:run
```

Verify:

```powershell
curl.exe -i http://localhost:8081/api/v1/ping
curl.exe -i http://localhost:8081/actuator/health
```

## Tests

Run from `ingestion-service`. Docker Desktop and the configured PostgreSQL test database are needed for database-backed tests; HTTP mock tests do not call Frankfurter over the network.

```powershell
.\mvnw.cmd "-Dtest=FrankfurterClientTest,FrankfurterClientResilienceTest" test
.\mvnw.cmd "-Dtest=BackfillServiceTest,IngestionControllerTest,IngestionErrorHandlerTest" test
.\mvnw.cmd "-Dtest=FxRateIngestionPostgresTest" test
.\mvnw.cmd verify
```

The existing `FxRateRepositoryTest` uses a separate `fxpulse_migration_test` database. Create it once if needed from the repository root:

```powershell
docker compose exec db psql -U fxpulse_user -d postgres -c "CREATE DATABASE fxpulse_migration_test;"
```

`fxpulse_user` is an example username; use the value configured in your own `.env`. More local setup details: [docs/setup.md](docs/setup.md).

For the Python proof of concept, from repository root:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r analytics/requirements.txt
.\.venv\Scripts\python.exe analytics/01_api_exploration.py
```

To stop services safely:

```powershell
docker compose stop db
# Or stop containers and network while retaining DB data:
docker compose down
```

Do not use `docker compose down -v` unless intentionally deleting the local database volume.

## Documentation and milestones

- [Development log](docs/LOG.md)
- [Data contract](docs/data-contract.md)
- [Architecture](docs/architecture.md)
- [Local setup](docs/setup.md)
- [API exploration](docs/api-exploration.md)
- [ECB provider selection](docs/adr/0001-source.md)

The design/prototype milestone (`v0.1-design`) established the foundation. The Java MVP release will be considered ready when ingestion, read API, Docker demo, full test suite and CI are demonstrated together. A pushed commit whose local tests passed is not the same as a green automated CI run.

## License

MIT - see [LICENSE](LICENSE).
