# Clock In

Clock In is a single-user worklog consisting of a Kotlin/Spring Boot API, an Angular frontend, and PostgreSQL persistence. It records time entries, calculates a running work-time balance, and represents vacation, sickness, and other absences as all-day events in the bookings table.

## Architecture

| Component | Technology | Runtime responsibility |
| --- | --- | --- |
| `web/` | Angular 21, TypeScript, Tailwind CSS | Check-in UI, bookings table, event and entry editing, settings |
| `api/` | Kotlin, Spring Boot 4, Spring Data JPA | Worklog API, validation, balance calculation, snapshots, holiday integration |
| `postgres` | PostgreSQL 18 | Workdays, time entries, events, settings, and the balance snapshot |
| `caddy` | Caddy 2 | Serves the web app and routes `/api/**` to the API |

The application has no authentication or per-user ownership model. It is intended for one person and must be placed behind an authenticated reverse proxy or restricted to a trusted network when deployed.

## Domain behavior

- `Workday` groups entries and calculated totals for one unique date.
- Standard entries contain start/end timestamps. Correction entries contain a signed duration.
- The bookings table is the canonical historical view and editor. `/calendar` is retained only as a redirect to `/bookings`.
- Events use inclusive start/end dates and affect whether covered dates count as workdays. Supported types are `vacation`, `sick`, and `other`.
- The API stores a singleton balance snapshot pointing at the latest calculated day. Time-entry and event mutations invalidate an affected checkpoint before recalculation, after which the checkpoint advances again.
- Settings and snapshots use fixed singleton IDs; workdays, entries, and events use generated IDs.

## Integrated deployment

Set a PostgreSQL password and start the published application images:

```bash
export POSTGRES_PASSWORD='replace-with-a-secure-value'
docker compose up -d
```

Open [http://localhost:3000](http://localhost:3000). Caddy sends `/api/**` to the API after removing the `/api` prefix and sends all other paths to the web container. These two routes are defined explicitly in `Caddyfile`; Caddy does not need access to the Docker socket.

`docker-compose.yml` pulls the `ghcr.io/dhoinka/clock-in/api` and `ghcr.io/dhoinka/clock-in/web` images; it does not build the current checkout. PostgreSQL data is retained in the `postgres-data` volume.

The current Liquibase changelog is a fresh single-user baseline. It does not migrate databases from the earlier multi-user model.

## Source development

Requirements:

- Java 21
- Node.js 24 and npm
- PostgreSQL for production-profile development; backend tests use H2

Start the backend from `api/`:

```bash
./gradlew bootRun
```

Start the frontend from `web/` in another terminal:

```bash
npm ci
npm start
```

The frontend runs on [http://localhost:4200](http://localhost:4200). Its development proxy forwards `/api/**` to [http://localhost:8080](http://localhost:8080) and strips `/api`.

Run verification before submitting changes:

```bash
cd api
./gradlew test

cd ../web
npm test -- --watch=false
npm run lint
npm run build
```

See [`api/README.md`](api/README.md) and [`web/README.md`](web/README.md) for component-specific contracts and implementation details.

## CI and images

Pushes to `main` run backend and frontend tests, build both container images for AMD64 and ARM64, and publish multi-platform `latest` plus commit-SHA tags to GitHub Container Registry. The API image packages the Spring Boot JAR on Eclipse Temurin, while the frontend image is served by Nginx.

## License

Clock In is licensed under the GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later). See `LICENSE`.
