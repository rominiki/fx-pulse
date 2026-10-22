# FX Pulse - Local PostgreSQL Setup

## Purpose

PostgreSQL runs as a Docker Compose service.

Database: fxpulse
Default local port: 5432
PostgreSQL major version: 16

## Requirements

- Docker Desktop with Linux containers
- Docker Compose
- A local .env file

## Configuration

Copy .env.example to .env and replace the
placeholder password with a strong local password.

Never commit .env to Git.

## Start PostgreSQL

From the FX Pulse repository root:

    docker compose up -d db

## Check Status

    docker compose ps

Expected: PostgreSQL reports healthy.

## Verify Database Connectivity

    docker compose exec db psql -U fxpulse_user -d fxpulse -c "SELECT version();"

## Interactive SQL

    docker compose exec db psql -U fxpulse_user -d fxpulse

Exit with \q.

## View Logs

    docker compose logs --tail=40 db

## Stop and Restart

    docker compose stop db
    docker compose start db

## Remove Containers

    docker compose down

The named volume is retained.

WARNING:
docker compose down -v removes Compose-managed
volumes and may permanently delete local data.

## Current Limitations

The database server is running, but FX Pulse
tables have not yet been created.

Flyway migrations will be implemented later.
