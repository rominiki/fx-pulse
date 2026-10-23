# FX Pulse - Local Development Setup

## Overview

FX Pulse is a Java Spring Boot and Data Engineering
project for collecting and processing foreign-exchange
reference rates.

The backend uses:

- Java 21
- Spring Boot 4.1.1
- Maven Wrapper
- PostgreSQL 16
- Docker Compose
- Flyway
- Spring Data JPA

## Prerequisites

Install and verify:

- Git
- Java 21
- Docker Desktop with the Docker engine running

You do not need a separate Maven installation
when using the included Maven Wrapper.

## 1. Clone the repository

```powershell
git clone https://github.com/rominiki/fx-pulse.git
cd fx-pulse
```

## 2. Configure the environment

Copy the environment template:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace the placeholder
database password with your own local password.

For an existing PostgreSQL volume, use the
password with which the database was initialized.

The default development database URL is:

```text
jdbc:postgresql://localhost:5432/fxpulse
```

Never commit `.env`.

## 3. Start PostgreSQL

From the repository root:

```powershell
docker compose up -d db
docker compose ps
```

Wait until the database becomes healthy.

Verify the connection:

```powershell
docker compose exec db psql -U fxpulse_user -d fxpulse -c "SELECT version();"
```

## 4. Prepare the test database

The repository integration test uses a separate database:

`fxpulse_migration_test`

Create it if it doesn't already exist:

```powershell
docker compose exec db psql -U fxpulse_user -d postgres -c "CREATE DATABASE fxpulse_migration_test;"
```

Flyway creates the required schema when the test
application starts.

Skip database creation if it already exists.

## 5. Load local environment variables

Enter the backend folder:

```powershell
cd ingestion-service
```

Load configuration from the parent `.env`:

```powershell
Get-Content ..\.env |
    Where-Object {
        $_ -match '^(DB_URL|DB_USER|DB_PASSWORD|FX_API_URL|SERVER_PORT)='
    } |
    ForEach-Object {
        $name, $value = $_ -split '=', 2
        [Environment]::SetEnvironmentVariable(
            $name, $value, "Process"
        )
    }
```

This assumes simple unquoted KEY=value lines.

Environment variables are local to that terminal session.
Load them again when opening a new terminal.

## 6. Start Spring Boot

From ingestion-service:

```powershell
.\mvnw.cmd spring-boot:run
```

The default local server port is 8081.

Flyway automatically validates and applies pending
database migrations at application startup.

## 7. Verify the API

Ping endpoint:

```powershell
curl.exe -i http://localhost:8081/api/v1/ping
```

Expected JSON:

```json
{"status":"ok"}
```

Actuator health endpoint:

```powershell
curl.exe -i http://localhost:8081/actuator/health
```

Expected JSON when healthy:

```json
{"status":"UP"}
```

## 8. Run tests

Keep PostgreSQL running and load the environment
variables into your test terminal.

From ingestion-service:

```powershell
.\mvnw.cmd clean test
```

Tests must complete without failures.

The FxRateRepositoryTest runs against
fxpulse_migration_test rather than the main database.

## 9. Stop services

Stop Spring Boot using Ctrl+C.

Return to the repository root:

```powershell
cd ..
```

Stop PostgreSQL:

```powershell
docker compose stop db
```

To remove Compose containers while preserving
the database volume:

```powershell
docker compose down
```

Do not use `docker compose down -v` unless you
intentionally want to remove local database data.

## 10. Troubleshooting

### Database authentication fails

Check that DB_USER and DB_PASSWORD are loaded
in the same terminal used to start Spring Boot.

Do not put passwords directly in application.properties.

### Port 8081 is busy

Stop the other application or set SERVER_PORT
to an available local port.

### Flyway fails

Check that PostgreSQL is running, the datasource
URL is correct and migration SQL files are
under src/main/resources/db/migration.

Do not modify already-applied Flyway migrations.

### JPA schema validation fails

Compare the entity column mappings to the
PostgreSQL schema created by Flyway.

Hibernate must validate the schema, not modify it.

## Current Project Scope

Completed backend foundation:

- Spring Boot service
- PostgreSQL in Docker Compose
- Flyway V1 schema migration
- Exchange-rate JPA entity
- Composite identifier
- Spring Data JPA repository
- DTO and mapper
- Integration tests
- Ping and health endpoints

Future work:

- Frankfurter API client
- Historical FX data ingestion
- Duplicate and conflict handling
- Airflow orchestration
- Financial analytics
