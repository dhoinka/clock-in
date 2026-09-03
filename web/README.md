# Clock In Web

Angular frontend for the Clock In worklog app.

## Requirements

- Node.js
- npm
- Clock In API running on `http://localhost:8080`

## Commands

```bash
npm ci                    # install dependencies
npm start                 # run on http://localhost:4200
npm test -- --watch=false # run tests once
npm run lint              # lint templates and TypeScript
npm run build             # production build
```

The development server proxies `/api` requests to the local API. Production
build output is written to `dist/`.

## Structure

- `src/app/core` — models, services, guards, and interceptors
- `src/app/features` — application features
- `src/app/layout` — application shell and navigation
- `src/app/shared` — shared components and utilities
