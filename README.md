
# FX Pulse — Foreign Exchange Data Platform

> Status: Work in progress
> Java Backend + Data Engineering portfolio project

FX Pulse is an end-to-end financial data platform designed
to collect, validate, store, and analyze historical and
daily foreign-exchange rates.

The goal is to build a reliable application combining
Java backend services with automated data pipelines,
analytical transformations, and data quality checks.

## Features

### Core platform (planned)
- Historical and daily exchange-rate ingestion
- Java 21 and Spring Boot REST services
- PostgreSQL storage with Flyway migrations
- Idempotent data ingestion and error handling
- SQL and Python-based financial calculations
- Apache Airflow pipeline orchestration
- Data quality checks and reconciliation
- Snowflake integration
- REST API for historical and analytical data
- Interactive dashboard
- Unit and integration tests
- Docker and GitHub Actions CI/CD

### Advanced extensions (planned)
- dbt analytical models and data tests
- Kafka event-driven processing
- Apache Spark historical data processing
- Databricks and Delta tables
- Kubernetes deployment

## Data Source

Exchange-rate data is obtained through the public
Frankfurter API.

FX Pulse uses the Frankfurter API v2 with the European
Central Bank (ECB) provider for historical and daily
reference exchange rates.

https://frankfurter.dev/

The platform uses daily reference-rate data,
not live trading prices.

## Technology Stack

Backend:
Java 21, Spring Boot, REST, JPA, Flyway

Data Engineering:
Python, SQL, Apache Airflow, Snowflake

Database:
PostgreSQL

Testing:
JUnit 5, Mockito, Testcontainers, pytest

DevOps:
Docker, GitHub Actions

Advanced:
dbt, Kafka, Spark, Databricks, Kubernetes

## Engineering Goals

- Reliable and repeatable historical ingestion
- No duplicate rates after pipeline reruns
- Correct handling of missing trading days
- Tested analytical calculations
- Recoverable pipeline failures
- Reproducible local development
- Clear API and architecture documentation

## Project Roadmap

The project is developed incrementally.

Phase 1: Java Backend
Phase 2: Core Data Engineering
Phase 3: Advanced Data Engineering
Phase 4: Deployment and Portfolio

See IMPLEMENTATION_PLAN.md for the full plan.

## Getting Started

Setup instructions and runnable commands will be
added as the implementation progresses.

## About

Independent portfolio project demonstrating
Java Backend Development, Data Engineering,
financial-data processing, and software testing.
