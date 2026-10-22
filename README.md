# FX Pulse - Foreign Exchange Data Platform

> **Status:** Work in progress - v0.1 Design & Python Prototype  
> **Focus:** Java Backend Development + Data Engineering portfolio project

FX Pulse is an end-to-end financial data platform designed to collect, validate, store, and analyze historical and daily foreign-exchange reference rates.

The goal is to build a reliable application combining Java backend services with automated data pipelines, analytical transformations, and data quality checks.

## Current Status - v0.1 Design & Prototype

FX Pulse is under active development. This initial milestone contains the project design and a working Python analytics prototype; it is **not** a completed data platform.

### Implemented

- Frankfurter API v2 exploration using the European Central Bank (ECB) provider
- Historical EUR/USD reference-rate retrieval via HTTP
- Basic JSON structure and value validation
- Python/pandas analytics prototype
- Percentage change between consecutive observations
- Five-observation moving average
- CSV export of analytical results
- Initial financial data contract and planned PostgreSQL schema
- System architecture documentation and an architecture decision record

### Not Yet Implemented

- Java Spring Boot ingestion service
- PostgreSQL persistence and Flyway migrations
- Automated ingestion, retries, and idempotent database writes
- REST API endpoints
- Apache Airflow orchestration
- Production data quality checks and reconciliation
- Interactive dashboard
- Automated CI/CD and deployment

## Features and Roadmap

### Phase 1 - Java Backend (planned)

- Historical and daily exchange-rate ingestion
- Java 21 and Spring Boot REST services
- PostgreSQL storage with Flyway migrations
- Idempotent writes and robust error handling
- Unit and integration testing
- Docker-based local development

### Phase 2 - Core Data Engineering (planned)

- SQL and Python financial calculations
- Apache Airflow pipeline orchestration
- Data quality checks and reconciliation
- Interactive dashboard
- Optional Snowflake integration

### Phase 3 - Advanced Data Engineering (optional extensions)

- dbt analytical models and data tests
- Kafka event-driven processing
- Apache Spark historical processing
- Databricks and Delta Lake experiments

### Phase 4 - Deployment and Portfolio (planned)

- GitHub Actions CI/CD
- Kubernetes deployment experiments
- End-to-end documentation, demonstrations, and project polish

## Data Source

FX Pulse uses [Frankfurter API v2](https://frankfurter.dev/) with the **European Central Bank (ECB)** provider for historical and daily reference exchange rates.

The provider-specific endpoint is:

```text
https://api.frankfurter.dev/v2/providers/ecb/rates
```

These are **reference rates**, not live trading prices. The source observation date is preserved, and non-publication days (such as weekends and holidays) are not filled with invented rates.

## Technology Stack - Current & Planned

| Area | Technologies | Status |
| --- | --- | --- |
| Analytics prototype | Python 3.13, pandas, requests | Implemented |
| Backend | Java 21, Spring Boot, REST, JPA, Flyway | Planned |
| Database | PostgreSQL | Planned |
| Data engineering | Python, SQL, Apache Airflow | Planned |
| Testing | JUnit 5, Mockito, Testcontainers, pytest | Planned |
| DevOps | Docker, GitHub Actions | Planned |
| Optional warehouse | Snowflake | Planned / optional |
| Advanced extensions | dbt, Kafka, Spark, Databricks, Kubernetes | Planned / optional |

The Python prototype was tested locally with **Python 3.13.5**, **pandas 3.1.0**, and **requests 2.34.2**. Java, Maven, and Docker are installed for the upcoming backend phase, but are not required to run the current prototype.

## Engineering Goals

- Reliable and repeatable historical ingestion
- No duplicate rates after pipeline reruns
- Correct handling of missing publication days
- Tested analytical calculations
- Recoverable pipeline failures
- Reproducible local development
- Clear API and architecture documentation

## Getting Started

These instructions cover the **currently implemented Python analytics prototype**.

### Prerequisites

- Python 3.13 (tested locally)
- Git
- Internet connection

### 1. Clone the repository

```powershell
git clone https://github.com/rominiki/fx-pulse.git
cd fx-pulse
```

If the repository is private, GitHub authentication and access permission are required.

### 2. Create a Python virtual environment

On Windows PowerShell:

```powershell
python -m venv .venv
```

### 3. Install dependencies

```powershell
.\.venv\Scripts\python.exe -m pip install -r analytics/requirements.txt
```

### 4. Run the analytics prototype

```powershell
.\.venv\Scripts\python.exe analytics/01_api_exploration.py
```

### 5. Review the results

The script:

1. Requests historical ECB EUR/USD reference rates.
2. Validates the response and sorts observations chronologically.
3. Calculates percentage changes between consecutive observations.
4. Calculates five-observation moving averages.
5. Exports the results to `analytics/eur_usd_jan_2025.csv`.

The first percentage return is missing because there is no preceding observation in the DataFrame. The first four moving averages are missing because five observations are required.

**Date-range note:** During local testing, the API returned a December 31, 2024 observation alongside January 2025 observations. Verify the dates in the generated CSV. If strict January-only output is required, apply a date filter *before* calculating returns and moving averages.

Run the script a second time to confirm it completes successfully and regenerates the CSV without appending duplicate rows.

## Documentation

- [API exploration](docs/api-exploration.md)
- [Financial data contract](docs/data-contract.md)
- [System architecture](docs/architecture.md)
- [Architecture decision: ECB source](docs/adr/0001-source.md)
- [Development log](docs/LOG.md)

## About

FX Pulse is an independent portfolio project demonstrating Java Backend Development, Data Engineering, financial-data processing, and software testing as the platform develops.

Development progress is tracked through GitHub Issues and the FX Pulse Development Project Board.
