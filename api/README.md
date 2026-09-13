# Clock In API

The Clock In API is a Kotlin/Spring Boot 4 service for a single global worklog. It exposes JSON endpoints for check-in state, workdays, time entries, all-day events, holidays, settings, and summary statistics.

## Runtime stack

- Java 21 toolchain
- Kotlin 2.4
- Spring Boot Web, Validation, Data JPA, Actuator, Jackson, and Liquibase
- PostgreSQL 18 in production
- H2 for tests

The service listens on port 8080 by default. Endpoint mappings do not contain an `/api` prefix; the frontend development proxy and Traefik add that external routing prefix.

## Running and verification

```bash
./gradlew bootRun       # development server on http://localhost:8080
./gradlew test          # unit and Spring/MockMvc integration tests
./gradlew clean build   # clean compilation, tests, and artifact build
```

Activate the `production` profile and configure PostgreSQL with:

```text
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
```

Liquibase applies `src/main/resources/db/changelog/db.changelog-master.xml` at startup. The existing change set is the single-user baseline; add a new change set for future schema changes.

## HTTP API

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/status` | Current check-in state, gross time, and running balance |
| `POST` | `/status` | Record the next check-in or check-out |
| `GET` | `/workdays/{yyyy-MM}` | Calculated rows for a month; defaults to the current month |
| `GET` | `/workdays/export` | Complete calculated worklog |
| `DELETE` | `/workdays` | Delete all workdays and entries |
| `GET` | `/entries?type=standard\|correction` | List entries, optionally filtered by type |
| `PUT` | `/entries` | Replace all entries for one date |
| `DELETE` | `/entries/{id}` | Delete one entry |
| `GET` | `/events` | List all events |
| `GET` | `/events?from=yyyy-MM-dd&to=yyyy-MM-dd` | List events overlapping an inclusive range; both parameters are required |
| `POST` | `/events` | Create an all-day event |
| `PUT` | `/events/{id}` | Update an event |
| `DELETE` | `/events/{id}` | Delete an event |
| `GET` | `/holidays/{year}` | Resolve public holidays; defaults to the current year |
| `GET/PUT` | `/settings` | Read or replace working hours, break time, and working-day bitmask |
| `GET` | `/worklog/stats` | Average start/end statistics |
| `GET` | `/actuator/health` | Health endpoint exposed by Actuator |

Dates use ISO `yyyy-MM-dd`; standard entry timestamps use ISO local date-time values. Duration strings are signed combinations of days, hours, minutes, seconds, and milliseconds, for example `8h`, `-30m`, or `1h 15m`.

An event request uses this contract:

```json
{
  "title": "Annual leave",
  "type": "vacation",
  "start": "2026-09-14",
  "end": "2026-09-18",
  "allDay": true
}
```

`title`, `type`, `start`, and `end` are required. The end date must not precede the start date. The API enum accepts `none`, `vacation`, `sick`, and `other`; the web booking flow exposes the latter three. The service forces `allDay` to `true` and assigns `new` status when creating an event.

Holiday data is fetched from the Spiketime Feiertag API, cached per year, and filtered to nationwide or Rhineland-Palatinate holidays. Remote failures are logged and produce an empty holiday list.

## Balance calculation and snapshots

`WorklogService` calculates each day chronologically from configured working hours, break time, working weekdays, holidays, events, standard entries, and corrections. Calculated gross and balance values are persisted on `Workday`.

The singleton `Snapshot` is a cached balance checkpoint:

1. Reads begin calculation at the snapshot workday instead of loading the complete history.
2. A time-entry mutation at or before that date invalidates the checkpoint before changing data.
3. Event mutations invalidate the checkpoint because events change workday classification.
4. After a successful balance calculation, the snapshot advances to the latest calculated day and never moves backwards.

When changing anything that can alter historical balances, preserve this invalidation-before-mutation and advancement-after-calculation ordering and add regression tests for it.

## Error handling

Expected client failures use typed exceptions from `shared/exception` and return structured 4xx responses. Unexpected exceptions are logged with their stack trace and returned as a generic HTTP 500 response; internal exception details must not be exposed to clients.

## Source layout

```text
src/main/kotlin/com/gloomstone/clockin/
├── config/                 Spring configuration
├── shared/exception/       API error types and global handler
└── worklog/
    ├── controller/         HTTP endpoints
    ├── domain/             JPA entities and enums
    ├── dto/                Transport contracts
    ├── mapper/             Entity/DTO conversion
    ├── repository/         Spring Data repositories
    ├── service/            Worklog behavior and integrations
    └── util/               Duration formatting and parsing
```

The API is intentionally unauthenticated. Do not add user IDs or ownership filters to domain records; protect access outside the application.
