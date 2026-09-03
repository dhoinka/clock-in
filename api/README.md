# Clock In API

Kotlin and Spring Boot API for the Clock In worklog app.

## Requirements

- Java 21
- PostgreSQL for production; tests use H2

## Commands

```bash
./gradlew bootRun       # run on http://localhost:8080
./gradlew test          # run tests
./gradlew clean build   # build and verify
```

Run PostgreSQL-backed configurations with the `production` profile and provide:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` — token-signing secret; required outside local development

Database migrations run through Liquibase at startup.

## Structure

- `src/main/kotlin/com/gloomstone/clockin/worklog` — worklog domain and API
- `src/main/kotlin/com/gloomstone/clockin/iam` — authentication and users
- `src/main/kotlin/com/gloomstone/clockin/shared` — shared security and errors
- `src/main/resources/db/changelog` — Liquibase migrations
- `src/test` — unit and integration tests
