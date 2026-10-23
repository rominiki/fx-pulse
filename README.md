# FX Pulse - Foreign Exchange Data Platform

> **Status:** Active Development - Java Backend Foundation  
> **Latest completed milestone:** v0.1 - Design & Python Prototype  
> **Focus:** Java Backend Development + Data Engineering  
> **Primary language:** Java 21  
> **Data source:** European Central Bank (ECB) reference rates via Frankfurter API v2

FX Pulse is an end-to-end financial data platform designed to collect, validate, store, and analyze historical and daily foreign-exchange reference rates.

The goal is to build a reliable application combining **Java backend services**, **PostgreSQL persistence**, **automated data pipelines**, **analytical transformations**, and **data quality checks**.

The project is being developed incrementally, with an emphasis on clean architecture, reliable data processing, reproducibility, automated testing, and engineering best practices.

---

## Current Status - Java Backend Foundation

FX Pulse is under active development.

The initial **v0.1 milestone** established the system architecture, financial data contract, and a working Python analytics prototype.

The current development stage focuses on building a Java Spring Boot backend connected to PostgreSQL.

### Implemented

**Data Exploration & Python Analytics**

- Frankfurter API v2 exploration using the ECB provider
- Historical EUR/USD reference-rate retrieval using HTTP
- Basic JSON response validation
- Python/pandas analytics prototype
- Percentage-change calculations between consecutive observations
- Five-observation moving averages
- CSV export of analytical results
- Initial API exploration documentation

**Java Backend**

- Java 21 Spring Boot 4.1.1 application structure
- Maven-based project with Maven Wrapper
- Spring Web, Spring Data JPA, Hibernate, and Flyway dependencies
- `FxRate` JPA entity
- `FxRateId` embedded composite primary key
- `FxRateRepository` interface with a date-range query
- `FxRateDto` record
- `FxRateMapper` for entity-to-DTO conversion
- JPA repository integration test implementation
- Lombok support for reducing Java boilerplate

**Database & Infrastructure**

- PostgreSQL 16 running through Docker Compose
- Persistent PostgreSQL Docker volume
- Environment-based database configuration
- Flyway V1 database migration
- `raw.fx_rates` table
- Composite primary key and database constraints
- Automatic schema migration execution
- Flyway migration history tracking
- Separate PostgreSQL database for integration testing

**Documentation & Design**

- Financial data contract
- System architecture documentation
- API exploration notes
- Architecture decision record (ADR)
- Development log
- GitHub Issues for development planning

### In Progress

- Finalizing Hibernate/JPA schema mapping validation
- Getting the full PostgreSQL integration test suite passing
- Finalizing externalized Spring Boot configuration
- Verifying REST ping and Actuator health endpoints
- Completing reproducible local setup documentation
- Finalizing the backend milestone

### Not Yet Implemented

- Java HTTP client for the Frankfurter API
- Automated historical and daily exchange-rate ingestion
- Retry handling and error recovery
- Idempotent ingestion and duplicate/conflict handling
- REST endpoints for retrieving stored exchange rates
- Apache Airflow orchestration
- Production data quality checks and reconciliation
- Interactive analytics dashboard
- Automated CI/CD and deployment

---

## System Architecture

FX Pulse is designed to separate data ingestion, persistent storage, API access, and analytical processing.

The planned end-to-end data flow is:

    Frankfurter API (ECB)
            |
            v
    Java Spring Boot Ingestion Service
            |
            v
    Data Validation & Normalization
            |
            v
    PostgreSQL - raw.fx_rates
            |
            +-----------------------+
            |                       |
            v                       v
    Spring Boot REST API      SQL / Python Analytics
            |                       |
            v                       v
    API Consumers             Reports & Dashboard

Apache Airflow will eventually orchestrate recurring ingestion and analytical workflows.

### Design Principles

- Separation of ingestion, persistence, and analytics
- Reproducible database schema management
- Preservation of source observation dates
- Explicit handling of missing publication days
- Consistent decimal precision for financial values
- Idempotent and recoverable data ingestion
- Externalized configuration
- Automated testing and data quality checks

The complete architecture is documented in [docs/architecture.md](docs/architecture.md).

---

## Features and Roadmap

### Phase 1 - Java Backend & Database (In Progress)

- [x] Java 21 Spring Boot project setup
- [x] Docker-based PostgreSQL development environment
- [x] Flyway V1 database migration
- [x] JPA entity and composite primary key implementation
- [x] Spring Data JPA repository implementation
- [x] DTO and entity mapper implementation
- [x] Initial repository integration test implementation
- [ ] Verify all repository integration tests pass
- [ ] Finalize backend configuration and health checks
- [ ] Implement Frankfurter API Java client
- [ ] Implement historical and daily exchange-rate ingestion
- [ ] Add idempotent writes and conflict handling
- [ ] Implement REST endpoints for exchange-rate retrieval
- [ ] Add comprehensive unit and integration testing

### Phase 2 - Core Data Engineering (Planned)

- [x] Initial Python/pandas analytics prototype
- [ ] SQL-based financial transformations
- [ ] Production-ready Python analytical processing
- [ ] Apache Airflow pipeline orchestration
- [ ] Automated scheduling and historical backfills
- [ ] Data quality validation and reconciliation
- [ ] Ingestion monitoring and failure recovery
- [ ] Interactive financial analytics dashboard
- [ ] Optional Snowflake integration

### Phase 3 - Advanced Data Engineering (Optional)

- [ ] dbt analytical models and data tests
- [ ] Kafka event-driven ingestion
- [ ] Apache Spark historical processing
- [ ] Databricks and Delta Lake experiments

### Phase 4 - Deployment & Portfolio (Planned)

- [ ] GitHub Actions CI/CD
- [ ] Automated build and testing pipeline
- [ ] Application containerization
- [ ] Deployment experiments
- [ ] Optional Kubernetes deployment
- [ ] End-to-end demonstrations and project documentation

---

## Data Source

FX Pulse uses [Frankfurter API v2](https://frankfurter.dev/) with the **European Central Bank (ECB)** provider for historical and daily foreign-exchange reference rates.

Provider-specific endpoint:

    https://api.frankfurter.dev/v2/providers/ecb/rates

These values are **reference exchange rates, not live trading prices**.

### Data Handling Principles

- Preserve the original source observation date.
- Do not fabricate rates for weekends or holidays.
- Validate incoming currency codes and rate values.
- Use decimal types appropriate for financial data.
- Preserve information about the data provider.
- Track when an observation was ingested.
- Detect duplicate observations and conflicting source values.

The Frankfurter API exploration is currently implemented in Python. The Java API client and automated ingestion will be added in a later development stage.

---

## PostgreSQL Data Model

FX Pulse stores raw foreign-exchange observations in the following PostgreSQL table:

`raw.fx_rates`

### Table Structure

| Column | PostgreSQL Type | Description |
|---|---|---|
| `provider` | `VARCHAR(16)` | Source provider, such as ECB |
| `rate_date` | `DATE` | Observation date published by the provider |
| `base_currency` | `CHAR(3)` | Base currency code |
| `quote_currency` | `CHAR(3)` | Quote currency code |
| `rate` | `NUMERIC(20,10)` | Foreign-exchange reference rate |
| `ingested_at` | `TIMESTAMPTZ` | Timestamp generated when the row is inserted |

### Composite Primary Key

The table uses the following columns as its primary key:

    (provider, rate_date, base_currency, quote_currency)

This ensures that the same provider, date, and currency pair cannot appear more than once.

The schema also includes constraints to reject nonpositive exchange rates and prevent the base and quote currencies from being identical.

### Database Migrations

Database schema changes are managed using Flyway.

The first migration is:

    ingestion-service/src/main/resources/db/migration/V1__create_fx_rates.sql

Flyway records applied migrations in its schema history table.

This allows developers to reproduce the database structure without manually executing SQL scripts.

The Java entity mappings use Hibernate/JPA, while **Flyway remains responsible for creating and modifying the database schema**.

---

## Technology Stack

| Area | Technologies | Status |
|---|---|---|
| Java backend | Java 21, Spring Boot 4.1.1 | Implemented foundation |
| Persistence | Spring Data JPA, Hibernate | Implemented; validation in progress |
| Database | PostgreSQL 16 | Implemented |
| Schema migrations | Flyway | Implemented |
| Local infrastructure | Docker Desktop, Docker Compose | Implemented |
| Java build tooling | Maven, Maven Wrapper | Implemented |
| DTO mapping | Java records, manual mapping | Implemented |
| Java testing | JUnit 5, Spring Boot Test | Tests implemented; verification in progress |
| Python analytics | Python 3.13, pandas, requests | Implemented prototype |
| Pipeline orchestration | Apache Airflow | Planned |
| Data transformation | Python, SQL | Prototype / planned |
| CI/CD | GitHub Actions | Planned |
| Integration test infrastructure | Testcontainers | Planned |
| Optional warehouse | Snowflake | Optional |
| Advanced extensions | dbt, Kafka, Spark, Databricks, Kubernetes | Optional |

### Tested Development Environment

The Python prototype was tested locally with:

- Python 3.13.5
- pandas 3.1.0
- requests 2.34.2

The Java backend is being developed using:

- Eclipse Temurin Java 21
- Spring Boot 4.1.1
- Maven Wrapper
- PostgreSQL 16
- Docker Desktop
- IntelliJ IDEA
- Windows PowerShell

---

## Getting Started

The repository currently contains two development components:

1. A Python analytics prototype that retrieves and processes historical exchange rates.
2. A Java Spring Boot backend with PostgreSQL persistence, currently under development.

### Prerequisites

- Git
- Java 21
- Docker Desktop with Docker Compose
- Python 3.13 for the analytics prototype
- Internet access for dependency installation and API requests

The Java backend includes Maven Wrapper, so a separate Maven installation is not required.

### 1. Clone the Repository

On Windows PowerShell:

```powershell
git clone https://github.com/rominiki/fx-pulse.git
cd fx-pulse
```

If the repository is private, GitHub authentication and access permissions are required.

---

## Running the Python Analytics Prototype

### 1. Create a Python Virtual Environment

From the repository root:

```powershell
python -m venv .venv
```

### 2. Install Dependencies

```powershell
.\.venv\Scripts\python.exe -m pip install -r analytics/requirements.txt
```

### 3. Run the Prototype

```powershell
.\.venv\Scripts\python.exe analytics/01_api_exploration.py
```

### 4. Review the Results

The script:

1. Requests historical ECB EUR/USD reference rates.
2. Validates the response and sorts observations chronologically.
3. Calculates percentage changes between consecutive observations.
4. Calculates five-observation moving averages.
5. Exports analytical results to `analytics/eur_usd_jan_2025.csv`.

The first percentage return is missing because there is no preceding observation in the DataFrame.

The first four moving averages are missing because five observations are required.

**Date-range note:** During local testing, the API returned a December 31, 2024 observation alongside January 2025 observations. Verify the generated CSV dates. For strict January-only output, filter the observations by date before calculating returns and moving averages.

Run the script a second time to confirm that it regenerates the CSV successfully rather than appending duplicate rows.

---

## Running PostgreSQL and the Java Backend

### 1. Create Your Local Environment Configuration

From the repository root, copy the example configuration:

```powershell
Copy-Item .env.example .env
```

Open `.env` and configure the local database credentials.

Expected environment variables:

| Variable | Purpose |
|---|---|
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `DB_URL` | JDBC URL for the main database |
| `FX_API_URL` | Frankfurter API endpoint |
| `SERVER_PORT` | Local Spring Boot server port |

The default local database URL is:

    jdbc:postgresql://localhost:5432/fxpulse

**Never commit `.env` or real credentials to GitHub.**

If you already have an initialized PostgreSQL Docker volume, use the original database password. Changing `.env` does not automatically change the existing PostgreSQL user's password.

### 2. Start PostgreSQL

Make sure Docker Desktop is running.

From the repository root:

```powershell
docker compose up -d db
docker compose ps
```

Wait until the database reports a healthy status.

Verify the connection:

```powershell
docker compose exec db psql -U fxpulse_user -d fxpulse -c "SELECT version();"
```

### 3. Load Environment Variables

Enter the Spring Boot project:

```powershell
cd ingestion-service
```

For Windows PowerShell, load the variables from the parent `.env` file:

```powershell
Get-Content ..\.env |
    Where-Object {
        $_ -match '^(DB_URL|DB_USER|DB_PASSWORD|FX_API_URL|SERVER_PORT)='
    } |
    ForEach-Object {
        $name, $value = $_ -split '=', 2

        [Environment]::SetEnvironmentVariable(
            $name,
            $value,
            "Process"
        )
    }
```

This assumes simple, unquoted `KEY=value` entries.

These variables are loaded into the current PowerShell session. New terminals must load them again.

Docker Compose and Spring Boot do not automatically share all environment variables, so the Java application requires this separate configuration step.

### 4. Start Spring Boot

From `ingestion-service`:

```powershell
.\mvnw.cmd spring-boot:run
```

The default development server port is 8081.

Spring Boot should connect to PostgreSQL, validate the Flyway migration history, initialize JPA, and start the web server.

### 5. Verify the Application

Ping endpoint:

```powershell
curl.exe -i http://localhost:8081/api/v1/ping
```

Expected JSON:

```json
{"status":"ok"}
```

Spring Boot Actuator health endpoint:

```powershell
curl.exe -i http://localhost:8081/actuator/health
```

Expected JSON when healthy:

```json
{"status":"UP"}
```


---

## Testing

FX Pulse uses JUnit 5 and Spring Boot Test for Java backend testing.

The repository integration test verifies that an exchange-rate entity can be stored in PostgreSQL and read back correctly.

### Repository Integration Test

The `FxRateRepositoryTest` covers:

- Saving an exchange-rate observation
- Retrieving an observation by its composite primary key
- Preserving observation dates and currency codes
- Preserving decimal precision with `BigDecimal`
- Reading the database-generated ingestion timestamp
- Querying observations by date range
- Converting an entity into an API DTO

### Test Database

Repository integration tests use a separate PostgreSQL database:

    fxpulse_migration_test

Create it once if it does not already exist:

```powershell
docker compose exec db psql -U fxpulse_user -d postgres -c "CREATE DATABASE fxpulse_migration_test;"
```

Run this from the repository root.

Flyway manages the schema in the test database independently of the main development database.

### Run the Tests

From `ingestion-service`, with PostgreSQL running and environment variables loaded:

Run the repository integration test:

```powershell
.\mvnw.cmd "-Dtest=FxRateRepositoryTest" test
```

Run the complete test suite:

```powershell
.\mvnw.cmd clean test
```

**Current verification status:** Java compilation and Flyway migration execution have succeeded. The complete JPA integration-test suite remains under verification; earlier runs encountered Hibernate column-type validation and database environment-variable configuration issues.

---

## Stopping Local Services

Stop Spring Boot using `Ctrl+C` in its running terminal.

From the repository root, stop PostgreSQL:

```powershell
docker compose stop db
```

To remove the Docker Compose containers and network while retaining the database volume:

```powershell
docker compose down
```

**Warning:** Avoid `docker compose down -v` unless you intentionally want to delete the Compose-managed volumes and their local database data.

---

## Engineering Goals

FX Pulse is designed to demonstrate practical backend and data engineering skills.

Key engineering objectives include:

- Reliable and repeatable historical exchange-rate ingestion
- No duplicate observations after pipeline reruns
- Explicit handling of missing publication days
- Correct decimal precision for financial values
- Version-controlled database schema evolution
- Tested data persistence and analytical calculations
- Recovery from pipeline failures
- Automated pipeline orchestration
- Reproducible local development
- Clear API and architecture documentation
- Maintainable, testable application components

---

## Documentation

Project documentation is maintained in the `docs/` directory.

- [API Exploration](docs/api-exploration.md)
- [Financial Data Contract](docs/data-contract.md)
- [System Architecture](docs/architecture.md)
- [Architecture Decision: ECB Data Source](docs/adr/0001-source.md)
- [Development Log](docs/LOG.md)
- [Local Development Setup](docs/setup.md)

---

## Development Milestones

### v0.1 - Design & Python Prototype

Initial project milestone.

Includes:

- API exploration
- Python analytics prototype
- Financial data contract
- System architecture design
- Architecture decision record
- Initial repository structure

### Java Backend Foundation

Current development stage.

Includes:

- Spring Boot service foundation
- Docker-based PostgreSQL
- Flyway database migrations
- JPA persistence model
- Composite primary key
- Spring Data repository
- DTO mapping
- Integration tests
- Externalized configuration
- Health endpoint verification

Final testing and sign-off are still in progress.

### Next Milestone - Exchange-Rate Ingestion

Planned work:

- Java Frankfurter API client
- Historical rate retrieval
- JSON parsing and validation
- Persistent exchange-rate ingestion
- Idempotent database operations
- Duplicate and conflict handling
- Integration tests for the ingestion pipeline

---

## About

FX Pulse is an independent portfolio project demonstrating **Java Backend Development**, **Data Engineering**, **financial-data processing**, **database design**, and **software testing**.

The project is intended to evolve from a working analytical prototype into a reproducible, tested, and automated financial data platform.

Development progress is tracked through GitHub Issues and the FX Pulse Development Project Board.

The platform is under active development, and features are documented according to their current implementation and verification status.