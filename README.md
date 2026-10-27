# FX Pulse

**Java 21 · Spring Boot 4.1.1 · PostgreSQL 16 · Docker Compose · GitHub Actions**

FX Pulse is a portfolio financial-data service for historical and daily **European Central Bank (ECB) reference exchange rates** fetched through [Frankfurter API v2](https://frankfurter.dev/). It validates source observations, stores them in PostgreSQL, supports repeatable historical backfills, and exposes a read-only REST API and basic statistics. **These are reference rates, not executable trading quotes or a real-time market feed.**

## Implemented Java backend

- ECB-specific Frankfurter v2 HTTP client with bounded retries for transient failures and structured upstream errors.
- Validated EUR-to-quote exchange rates, precise `BigDecimal` values, preserved source publication dates.
- PostgreSQL persistence using Flyway and an atomic unique-key insert. Identical reruns skip rows; conflicting source values are reported without silent overwrites.
- Manual single-date ingestion and bounded historical backfills (up to 366 inclusive days, processed in calendar-month windows).
- Paginated, sorted exchange-rate query endpoint and basic statistics query endpoint.
- OpenAPI/Swagger UI, health endpoint, JUnit/MockMvc/Mockito tests, PostgreSQL integration tests, Testcontainers, Docker Compose and GitHub Actions.
- Repeatable PowerShell end-to-end smoke test (`scripts/smoke-test.ps1`).

**Verified by the developer:** local test suite, application startup, historical backfill and duplicate-safe rerun, rates and statistics query with stored data, Docker execution, GitHub Actions CI, and smoke test. Public branch and release availability should be checked separately.

## Architecture

```text
Frankfurter v2 / ECB
         |
Spring RestClient (HTTP + retry)
         |
Ingestion and validation services
         |
PostgreSQL raw.fx_rates (Flyway migration; unique compound key)
         |
Spring REST API: historical rates + basic statistics
```

Python/pandas exploration is provided in `analytics/`. Scheduling with Airflow, production analytical transformations, Snowflake comparison and a dashboard are **future work**, not part of this Java MVP.

## Quick start (Docker)

Requirements: Docker Desktop with Docker Compose, Git.

```powershell
git clone https://github.com/rominiki/fx-pulse.git
cd fx-pulse
Copy-Item .env.example .env
```

Set `DB_USER` and `DB_PASSWORD` in the untracked `.env` file. Keep the database password already associated with an existing PostgreSQL data volume. `FX_API_URL` can remain the documented ECB endpoint. **Never commit `.env`.**

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
curl.exe http://localhost:8081/actuator/health
```

The root `docker-compose.yml` must contain **both** `db` and `ingestion-service`. If your checkout only defines `db`, sync your verified local Compose changes before using this instruction. Stop any IntelliJ-run application already bound to port 8081.

### Sample requests

```powershell
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion?date=2025-01-02&quote=USD"
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion/backfill?from=2025-01-01&to=2025-01-31&quote=USD"
curl.exe "http://localhost:8081/api/v1/rates?from=2025-01-01&to=2025-01-31&quote=USD&page=0&size=20"
curl.exe "http://localhost:8081/api/v1/stats/basic?from=2025-01-01&to=2025-01-31&quote=USD"
```

- Swagger UI: <http://localhost:8081/swagger-ui.html>
- OpenAPI: <http://localhost:8081/v3/api-docs>
- Health: <http://localhost:8081/actuator/health>

The rates endpoint returns `content`, `page`, `size`, `totalElements` and `totalPages`. The statistics endpoint returns observation count and date/rate summary fields; an empty interval returns zero observations and null statistics. Weekends and ECB holidays do not imply missing pipeline records.

## Verification

With Java 21 and the necessary test database/environment configuration, from `ingestion-service`:

```powershell
.\mvnw.cmd clean verify
```

On an Ubuntu CI runner: `./mvnw -B clean verify`. Existing repository tests may expect the separate database `fxpulse_migration_test`; the CI workflow provides PostgreSQL for those tests. Testcontainers tests use disposable databases.

With the full application running, from the repository root:

```powershell
.\scripts\smoke-test.ps1
```

The smoke test replays a backfill, checks that the repeat inserted zero rows, verifies nonempty rates and matching summary counts, and checks OpenAPI paths. Do not run the destructive `docker compose down -v` unless intentionally removing local data.

## Persistence contract

`raw.fx_rates` stores provider, published rate date, base currency, quote currency, exact decimal rate and ingestion timestamp. Unique key: `(provider, rate_date, base_currency, quote_currency)`. See [data contract](docs/data-contract.md).

## Project documentation

- [Setup and troubleshooting](docs/setup.md)
- [Data contract and conflict policy](docs/data-contract.md)
- [Architecture](docs/architecture.md)
- [Engineering log](docs/LOG.md)
- [Release notes](docs/release-notes.md)
- [ECB source decision](docs/adr/0001-source.md)

## Release and limitations

The intended Java MVP tag is `v0.4-java-mvp`. The tag is a release identifier, **not** the Spring Boot or database version. See [release notes](docs/release-notes.md).

Known boundaries: ECB data is not live trading data; the API offers basic summary statistics rather than production financial analytics; a manual ingestion endpoint exists but no continuous scheduler is included; this is a local/CI reproducible demo, not a hardened publicly hosted production service.

## License

MIT - see [LICENSE](LICENSE).
