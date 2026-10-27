# Local setup and verification

## Docker-first setup

Requirements: Git and Docker Desktop. From repository root:

```powershell
Copy-Item .env.example .env
```

Edit `.env`, setting `DB_USER` and `DB_PASSWORD` to local values; do not commit the file. For an existing PostgreSQL volume, changing `.env` alone does not reset the old database user's password.

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 ingestion-service
curl.exe http://localhost:8081/actuator/health
```

The running Compose configuration must include `db` and `ingestion-service`, with application `DB_URL=jdbc:postgresql://db:5432/fxpulse`. Inside Docker, use `db`, **not** `localhost`, as the PostgreSQL hostname. Wait for `db` health before application initialization. If port 8081 is occupied, stop the previously running IntelliJ application.

## Verify the full pipeline

```powershell
curl.exe -i -X POST "http://localhost:8081/api/v1/ingestion/backfill?from=2025-01-01&to=2025-01-31&quote=USD"
curl.exe "http://localhost:8081/api/v1/rates?from=2025-01-01&to=2025-01-31&quote=USD&page=0&size=20"
curl.exe "http://localhost:8081/api/v1/stats/basic?from=2025-01-01&to=2025-01-31&quote=USD"
.\scripts\smoke-test.ps1
```

A repeat of the same backfill should insert zero new observations. API docs are at `http://localhost:8081/swagger-ui.html` and `http://localhost:8081/v3/api-docs`.

## Maven / IntelliJ option

Start only PostgreSQL: `docker compose up -d db`. From `ingestion-service`, use JDK 21. IntelliJ's Spring Boot run configuration needs `DB_URL=jdbc:postgresql://localhost:5432/fxpulse`, `DB_USER`, `DB_PASSWORD`, `FX_API_URL=https://api.frankfurter.dev/v2/providers/ecb/rates`, and `SERVER_PORT=8081`, or load the corresponding `.env` entries into the **same** PowerShell process before running Maven.

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean verify
```

Your original repository integration test uses a separate `fxpulse_migration_test` PostgreSQL database; ensure it exists locally and test environment variables point to it when needed. GitHub Actions supplies a PostgreSQL service for CI.

## Lifecycle and troubleshooting

- `docker compose down` stops/removes containers while retaining the named volume.
- `docker compose down -v` removes named volumes and **deletes local database data**; avoid for routine stops.
- `url must start with jdbc`: check the `DB_URL` value in IntelliJ/PowerShell.
- `Port 8081 already in use`: stop the duplicate Spring Boot process.
- Empty `/rates` and zero `/stats/basic`: ingest a matching historical interval first.
- Query parameters must contain ordinary `&` separators; do not copy `%26` or `%3D` in place of query delimiters.
