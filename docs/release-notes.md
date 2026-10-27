# FX Pulse - Java backend MVP release notes

**Proposed release:** `v0.4-java-mvp`  
**Release title:** FX Pulse v0.4 - Java Backend MVP  
**Status:** Ready to tag after the latest committed `main` branch, CI and public documentation have been checked.

## Included

- Java 21 / Spring Boot 4.1.1 ECB reference-rate ingestion through Frankfurter v2.
- Source-date, currency and precise decimal validation.
- PostgreSQL 16 / Flyway persistence with duplicate-safe reruns and explicit conflicts.
- Manual single-date ingestion and recoverable monthly-window historical backfills.
- Filtered paginated historical rates and basic statistics endpoints.
- Structured errors, bounded upstream HTTP retries, OpenAPI and Swagger UI.
- Multi-stage Java 21 Dockerfile and Docker Compose application/database orchestration.
- JUnit/MockMvc/Mockito, PostgreSQL/Testcontainers integration coverage, GitHub Actions and PowerShell smoke test.

## Verification reported

Developer reports passing local test suite, successful Docker startup, green GitHub Actions, successful historical backfill/repeat, populated rates/statistics responses and passing smoke test. The published tag is not evidence of a hosted production deployment.

## Known limitations

Reference rates are not live FX prices. No automatic ingestion schedule, production analytics/financial signals, authentication/authorization for public hosting, or hosted production deployment is included. Advanced data engineering components remain planned.

## Upgrade and migration notes

The authoritative schema is Flyway `V1__create_fx_rates.sql`. Existing named Docker volumes persist across ordinary Compose restarts. Do not use `docker compose down -v` unless intentionally deleting local data.

## Demo evidence

Add real evidence links below **only when available**:

- [ ] 60–90 second screen recording (URL to add)
- [ ] Swagger screenshot (`docs/screenshots/swagger.png` if captured)
- [ ] Ingestion + idempotency screenshot (`docs/screenshots/backfill.png` if captured)
- [ ] GitHub Actions successful run (URL to add)
