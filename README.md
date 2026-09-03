# Clock In

Clock In is a simple worklog app.

Use it to record when you worked, keep track of your entries, and review your
logged time. That is the whole idea.

## Run it locally

Start the complete application from the repository root:

```bash
docker compose up -d
```

Once the containers are running, open [http://localhost:3000](http://localhost:3000).

The values in `docker-compose.yml` are intended for local development. Before
running the app anywhere else, provide secure values for `POSTGRES_PASSWORD`
and `APP_JWT_SECRET`.

## Development

The project consists of:

- `api/` — Kotlin and Spring Boot API
- `web/` — Angular frontend
- `docker-compose.yml` — local application stack

Run backend checks from `api/`:

```bash
./gradlew test
./gradlew clean build
```

Run frontend checks from `web/`:

```bash
npm ci
npm test -- --watch=false
npm run build
npm run lint
```

More details are available in `api/README.md` and `web/README.md`.

## License

Clock In is licensed under the GNU Affero General Public License v3.0 or later
(AGPL-3.0-or-later). See `LICENSE` for the full license text.
