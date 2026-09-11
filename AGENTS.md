# Clock In Contributor Guide

## Repository layout

- `api/`: Kotlin, Spring Boot, Gradle, PostgreSQL, and Liquibase.
- `web/`: Angular and TypeScript frontend.
- `docker-compose.yml`: local integrated stack and routing.
- `.github/workflows/build.yml`: main-branch build, test, image publishing, and package cleanup.

## Commands

Run backend commands from `api/`:

```bash
./gradlew test
./gradlew clean build
./gradlew bootRun
```

Run frontend commands from `web/`:

```bash
npm ci
npm test -- --watch=false
npm run build
npm run lint
```

Run the complete local stack from the repository root:

```bash
docker compose up -d
```

## Backend conventions

- Use Java 21 and follow the existing Kotlin and Spring patterns.
- The main packages remain `com.gloomstone.clockin.worklog` and `com.gloomstone.clockin.shared`.
- The application is intentionally single-user and unauthenticated. Do not add accounts, identities, roles, tenant keys, or ownership filters to worklog resources.
- Use the typed exceptions in `com.gloomstone.clockin.shared.exception`; do not expose internal exception details in API responses.
- Treat the current Liquibase changelog as the baseline schema for fresh installations. For future database changes, add new change sets rather than rewriting that baseline.
- Compose credentials are development-only dummies. Production values must come from operator-managed environment variables.

## Backend testing

- Integration tests use Spring Boot, MockMvc, and an H2 database.
- MockMvc integration tests call endpoints without authentication.

## Frontend conventions

- Keep TypeScript strict and avoid `any`; use `unknown` when a value is not yet typed.
- Use standalone Angular components, signals for local state, and native template control flow (`@if`, `@for`, and `@switch`).
- Prefer `inject()`, reactive forms, and `input()`/`output()` over decorator-based equivalents.
- Keep components focused and use `ChangeDetectionStrategy.OnPush`.
- Do not render untrusted content with `innerHTML`.
- Preserve keyboard navigation, visible focus, semantic markup, and WCAG AA contrast.

## Change discipline

- Keep changes scoped to the request and preserve unrelated worktree changes.
- Do not change dependency versions or lockfiles unless the task explicitly requires it.
- Add or update tests for behavior changes, then run the relevant component suite.
- For cross-component API changes, verify both the backend contract and the frontend consumer.
- Run `git diff --check` before handing work back.
