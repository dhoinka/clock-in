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
- The main packages remain `com.gloomstone.clockin.iam`, `com.gloomstone.clockin.worklog`, and `com.gloomstone.clockin.shared`.
- Inject the authenticated `UserPrincipal` with `@AuthenticationPrincipal`.
- Keep management endpoints under `/mgmt/**` restricted to the `admin` authority.
- Use `UserService.findByIdentity` for username, email, or ID lookup.
- Use the typed exceptions in `com.gloomstone.clockin.shared.exception`; do not expose internal exception details in API responses.
- Preserve resource ownership checks when reading, updating, or deleting user data.
- Self-service profile updates must never accept roles or account activation state.
- Add database changes as new Liquibase change sets. Do not rewrite migrations that may already have run.
- The repository intentionally seeds roles but no users. Tests must create their own users and must not depend on a seeded administrator.
- The checked-in JWT key and Compose credentials are development-only dummies. Production values must come from operator-managed environment variables.

## Backend testing

- Integration tests use Spring Boot, MockMvc, and an H2 database.
- Use `token()` from `com.gloomstone.clockin.shared.testutil` for authenticated MockMvc requests. `token()` has admin and manager authorities; `token("alice")` has no roles.
- Create any database user needed by a test inside that test or its setup method. A JWT principal does not create a corresponding database row.
- Mock `PasswordEncoder` with the helpers in `com.gloomstone.clockin.iam.util` where the surrounding test suite already follows that pattern.

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
