## Quick Start

## Local Development

Start Postgres only:

- `docker compose up -d postgres`

Run the backend locally:

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