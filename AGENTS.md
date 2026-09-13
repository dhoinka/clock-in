# Clock In Contributor Guide

## Repository layout

- `api/`: Kotlin, Spring Boot, Gradle, PostgreSQL, and Liquibase.
- `web/`: Angular and TypeScript frontend.
- `docker-compose.yml`: published web/API images, PostgreSQL, and Traefik routing for the integrated stack.
- `.github/workflows/build.yml`: main-branch build, test, image publishing, and package cleanup.

## Current product behavior

- The application is a single-user, unauthenticated worklog. Access control belongs at the deployment boundary.
- The bookings table is the canonical history view. The former calendar route redirects to `/bookings`; do not introduce a second calendar-based editing flow.
- A table row represents one date and can contain standard time entries, correction entries, holidays, and overlapping all-day events.
- Events are created and edited from the bookings table. Supported user-facing event types are `vacation`, `sick`, and `other`; newly created events have status `new`.
- Event ranges are inclusive. Their API dates use ISO `yyyy-MM-dd`, while time-entry timestamps use ISO local date-time values.

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

The frontend development server runs on port 4200 and proxies `/api/**` to the backend on port 8080, stripping the `/api` prefix.

Run the complete local stack from the repository root:

```bash
docker compose up -d
```

## Backend conventions

- Use Java 21 and follow the existing Kotlin and Spring patterns.
- The main packages remain `com.gloomstone.clockin.worklog` and `com.gloomstone.clockin.shared`.
- The application is intentionally single-user and unauthenticated. Do not add accounts, identities, roles, tenant keys, or ownership filters to worklog resources.
- Use the typed exceptions in `com.gloomstone.clockin.shared.exception`; do not expose internal exception details in API responses.
- Keep exception-handler methods in readable block form. Unexpected exceptions must be logged with their stack trace before returning a generic HTTP 500 response.
- Keep JSON field names aligned between Kotlin DTOs and TypeScript contracts. In particular, the event all-day field is `allDay` and must not be sent as `null`.
- Event mutations must invalidate the balance snapshot because events affect whether dates count as workdays.
- Treat the snapshot as a cached balance checkpoint. Invalidate it before changing a date at or before the checkpoint, recalculate balances, and advance it to the latest calculated day. A snapshot must never move backwards or preserve an edited checkpoint balance.
- Treat the current Liquibase changelog as the baseline schema for fresh installations. For future database changes, add new change sets rather than rewriting that baseline.
- Compose credentials are development-only dummies. Production values must come from operator-managed environment variables.

## Backend testing

- Integration tests use Spring Boot, MockMvc, and an H2 database.
- MockMvc integration tests call endpoints without authentication.
- Add regression coverage for snapshot invalidation/advancement whenever a change can affect historical balances.

## Frontend conventions

- Keep TypeScript strict and avoid `any`; use `unknown` when a value is not yet typed.
- Use standalone Angular components, signals for local state, and native template control flow (`@if`, `@for`, and `@switch`).
- Prefer `inject()`, reactive forms, and `input()`/`output()` over decorator-based equivalents.
- Keep components focused and use `ChangeDetectionStrategy.OnPush`.
- Keep bookings table data loading coordinated: workdays, ranged events, and holidays are fetched together for the selected month.
- Use the existing event badge colors and Lucide icons (`umbrella`, `heart-pulse`, and `sparkles`) consistently.
- Do not render untrusted content with `innerHTML`.
- Preserve keyboard navigation, visible focus, semantic markup, and WCAG AA contrast.

## Change discipline

- Keep changes scoped to the request and preserve unrelated worktree changes.
- Do not change dependency versions or lockfiles unless the task explicitly requires it.
- Add or update tests for behavior changes, then run the relevant component suite.
- For cross-component API changes, verify both the backend contract and the frontend consumer.
- Run `git diff --check` before handing work back.
