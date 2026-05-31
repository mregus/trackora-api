# FleetWise

FleetWise is an AI-powered fleet management platform focused on maintenance operations, fuel tracking, operational alerts, and AI-assisted fleet insights.

## Screenshots

### Dashboard
- Fleet KPIs
- AI fleet summaries
- Alert overview
- Fuel and maintenance charts

![Dashboard](/images/dashboard.png)

### Vehicle Details
- Vehicle analytics
- AI vehicle insights
- Maintenance history
- Vehicle document management

![Vehicles](/images/vehicles.png)

### Maintenance
- Maintenance scheduling
- Invoice/document uploads
- Status tracking

![Maintenance](/images/maintenance.png)

### Alerts
- Severity filtering
- Pagination
- Operational alerts

![Alerts](/images/alerts.png)

## Features

- JWT authentication
- Fleet and vehicle management
- Maintenance tracking and scheduling
- Fuel logging and anomaly alerts
- AI-generated fleet and vehicle summaries
- Operational alert center with severity levels
- Vehicle and maintenance document uploads
- Dashboard analytics and charts
- Pagination and filtering
- Swagger/OpenAPI docs
- Dockerized local runtime
- Integration test coverage

---

## Backend Stack

- Java 21
- Spring Boot 3.x
- Gradle
- PostgreSQL 16
- Flyway
- Spring Security
- JWT
- Spring Data JPA
- OpenAPI / Swagger
- Docker Compose
- Rest Assured integration tests

---

## Frontend

FleetWise includes an Angular frontend application providing:

- Dashboard analytics
- Vehicle management UI
- Maintenance management
- Fuel tracking
- Alerts center
- AI insights
- Document uploads
- Responsive Material UI

Frontend stack:

- Angular 19
- Angular Material
- RxJS
- Signals
- Chart.js

---

## Architecture

Frontend:
- Angular SPA

Backend:
- Spring Boot REST API

Database:
- PostgreSQL 16

Infrastructure:
- Docker Compose

---

## Document Management

Supports:
- Vehicle documents
- Maintenance invoices
- Image uploads
- Secure downloads
- File validation
- Tenant isolation

Documents stored on AWS S3 bucket:

```text
STORAGE_PROVIDER=s3
AWS_S3_BUCKET=trackora-documents
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

---

## Alerts

Alert types include:
- Maintenance due
- Maintenance overdue
- Fuel anomalies

Severity levels:
- CRITICAL
- WARNING
- INFO

## Project Structure

```text
src/main/java/com/fleetwise/api
 ├── auth
 ├── fleet
 ├── vehicle
 ├── maintenance
 ├── fuel
 ├── alert
 ├── ai
 ├── dashboard
 └── common
 ```

## Prerequisites

- Java 21
- Docker Desktop

## Local Development

Start Postgres only:

- `docker compose up -d postgres`

Run the API locally:

- `./gradlew bootRun`

Run with dev seed data:

- `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`

## Docker Runtime

Run PostgreSQL and API together:

- `docker compose up --build`

Stop services:

- `docker compose down`

Reset local database:

- `docker compose down -v`
- `docker compose up -d postgres`

## Database

Default local database:

```text 
Database: fleetwise
Username: fleetwise
Password: fleetwise
Port: 5432
```

Connect with psql:

- `docker exec -it fleetwise-postgres psql -U fleetwise -d fleetwise`

## Flyway Migrations

When running with the dev profile:

- `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`

Demo credentials:

```text
Email: demo@fleetwise.com
Password: Password123!
```

Seed data includes:

- Demo fleet
- 3 vehicles
- Maintenance records
- Fuel logs
- Alerts
- Mock AI insight

## OpenAI Configuration

By default, OpenAI is disabled for local development.

```text
openai:
  enabled: false
```

To enable real OpenAI calls:

- `export OPENAI_ENABLED=true`
- `export OPENAI_API_KEY=your_api_key_here`
- `./gradlew bootRun`

For tests and local demos, mock mode is recommended.

### API Documentation

Swagger UI:

http://localhost:8080/swagger-ui.html

OpenAPI JSON:

http://localhost:8080/v3/api-docs

## Authentication Flow

Register:

- `POST /api/auth/register`

Login:

- `POST /api/auth/login`

Current user:

```text
GET /api/auth/me
Authorization: Bearer <token>
```

Use the returned JWT token for protected endpoints.

## Running Tests

Run all tests:

- `./gradlew clean test`

Run one integration test:

- `./gradlew test --tests "*VehicleApiIT"`

Recommended test coverage includes:

1. Auth API
2. Fleet CRUD
3. Vehicle CRUD
4. Maintenance CRUD
5. Fuel logging
6. Alerts
7. Dashboard summary
8. Tenant isolation
9. AI insight generation

All fleet-scoped data is protected by ownership checks.

__Examples:__

```text
findByIdAndOwnerId(fleetId, ownerUserId)
findByIdAndFleetOwnerId(vehicleId, ownerUserId)
```

Users must not be able to access another user's:

- fleets
- vehicles
- maintenance records
- fuel logs
- alerts
- dashboard data
- AI insights

## Health Check

- `GET /actuator/health`

Expected:

```json
{
    "status": "UP"
}
```