# FX Pulse - Engineering log

This document records engineering changes by **capability**, not by classroom schedule. Detailed implementation history remains available in Git commits.

## Data-source discovery and design

Selected Frankfurter v2 using the ECB provider rather than blended publisher data. Inspected flat-array JSON, reference-rate publication gaps and historical date behavior. Established observation identity, decimal precision, source-date preservation and an architecture decision record. Prototyped EUR/USD percentage changes and five-observation moving averages in Python/pandas.

## Java backend and persistence foundation

Created the Java 21 Spring Boot project, Maven wrapper, health/ping endpoint, PostgreSQL 16 Compose development database, Flyway V1 schema, JPA `FxRate`/`FxRateId` model, repository and API DTO mapping. Configured environment-based connection values and repository tests. Verified migration replay safety.

## ECB ingestion and historical backfill

Added typed HTTP client response DTOs and client tests; implemented validation, transactional ingestion, atomic duplicate-safe database insert, explicit conflicting-source-value detection, single-date ingestion and bounded monthly-window backfill endpoints. Added safe rerun counts and recovery from interruption.

## HTTP resilience and automated testing

Added bounded retries for 429, 5xx and connection failures; separated permanent and transient upstream errors; added MVC, client, PostgreSQL and Testcontainers tests. Verified local tests as reported by the developer.

## Read-only API and statistics

Implemented paginated/sorted `GET /api/v1/rates` and `GET /api/v1/stats/basic`; added OpenAPI and Swagger UI. Verified the read endpoints first for empty data and again after historical ingestion, producing actual stored observations and summary statistics.

## Containerization and continuous integration

Built the Java service using a Java 21 multi-stage Dockerfile; configured service-to-service PostgreSQL networking and health dependency in Docker Compose. GitHub Actions runs Maven verification using Java 21 and PostgreSQL. The developer reports successful Docker startup, application health checks and green CI.

## End-to-end regression verification

Added `scripts/smoke-test.ps1` to exercise HTTP health, a historical backfill, idempotent rerun, stored rates, summary counts and OpenAPI discovery. The developer reports the smoke test passes.

## Release readiness

Before tagging, verify the **committed public branch** contains the updated Java Compose service, the scripts folder, CI workflow, and these release documents. Confirm the latest GitHub Actions run is green and that no credentials are tracked. Add actual screenshots/demo evidence if recorded. Release target: `v0.4-java-mvp`.

## Next development stage

SQL/Python production analytics, Airflow scheduling, data-quality reconciliation, optional Snowflake comparison and dashboard. These are not claimed as implemented parts of the Java MVP.
