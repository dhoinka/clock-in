# IAM (Identity & Access Management)

This service implements identity, authentication and user/role management for Clock In. It's a Kotlin + Spring Boot application built with Gradle.

This README covers local development, external dependencies, building Docker images and troubleshooting.

## Quick commands

Build the project:

```bash
./gradlew clean build
```

Run locally (development):

```bash
./gradlew bootRun
```

Run with the production profile:

```bash
SPRING_PROFILES_ACTIVE=production ./gradlew bootRun
```

## External dependencies (for local dev)

The service depends on a few external services during development. You can run simple Docker containers for each:

- PostgreSQL (database)

```bash
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres --name postgres postgres:18
```

Adjust credentials and ports as needed. The project expects database connection properties via Spring environment properties (see next section).

## Environment variables / Spring properties

Configure these values via `application.yml`, `.env`, or your shell environment. Common properties used during development:

- `SPRING_DATASOURCE_URL` — JDBC URL for Postgres (e.g. `jdbc:postgresql://localhost:5432/postgres`)
- `SPRING_DATASOURCE_USERNAME` — DB username (default: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` — DB password
- `SPRING_PROFILES_ACTIVE` — active Spring profile (for example, `production`)
- `APP_JWT_SECRET` — a random secret of at least 64 bytes for signing tokens

## Docker image

We build OCI images using Gradle's `bootBuildImage` (Spring Boot plugin) rather than building with the `Dockerfile` directly. Examples below show local and CI-style usage.

API (local build):

```bash
./gradlew clean bootBuildImage \
	--imageName=clock-in/api:local \
	--publishImage=false
# then run locally
docker run -e SPRING_DATASOURCE_PASSWORD=postgres -e APP_JWT_SECRET='<64-byte-secret>' -p 8080:8080 clock-in/api:local
```

API (publish to registry, used in CI):

```bash
./gradlew bootBuildImage \
	--imageName=ghcr.io/OWNER/clock-in/api:latest \
	--publishImage
```

Notes:
- CI uses `bootBuildImage` and Docker to push images to the registry (see `.github/workflows/build.yml`).
- The repository still contains a `Dockerfile` (alternative packaging), but prefer `bootBuildImage` for image builds used by CI and local reproducibility.

## Tests

Run unit and integration tests with Gradle:

```bash
./gradlew test
```

## Common troubleshooting

- Database migrations fail: ensure Postgres is reachable and credentials match `SPRING_DATASOURCE_*` properties.
- Port already in use: the application listens on port 8080 by default; change `server.port` or free the port.
- Healthcheck failing in Docker: check container logs with `docker logs <container>` and confirm `/actuator/health` returns UP.

## Where to look in the code

- `src/main/kotlin` — Kotlin source code
- `src/main/resources/application.yml` — Spring Boot configuration
- `build.gradle` — build configuration, dependencies and plugins
- `Dockerfile` — production image packaging

## Contributing

- Follow Kotlin and Spring Boot best practices.
- Add tests for new features and run `./gradlew test` before submitting PRs.
