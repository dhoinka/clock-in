# Clock In Web

The Clock In web client is an Angular 21 standalone application. It provides check-in controls, a table-first worklog editor, all-day event booking, and work-schedule settings.

## Runtime stack

- Node.js 24 and npm
- Angular 21 with standalone components and lazy routes
- TypeScript 5.9 in strict mode
- Angular signals and `ChangeDetectionStrategy.OnPush`
- Tailwind CSS 4 and local Zard-style shared components
- Lucide icons through `@ng-icons`
- Vitest through Angular's unit-test builder

## Commands

```bash
npm ci                    # reproducible dependency installation
npm start                 # development server on http://localhost:4200
npm test -- --watch=false # run unit tests once
npm run lint              # lint TypeScript and templates
npm run build             # production build in dist/client/browser
npm run format            # apply Prettier to the web workspace
```

The development server uses `proxy.conf.js`: browser requests to `/api/**` are forwarded to `http://localhost:8080` with the `/api` prefix removed. Start the API separately with `./gradlew bootRun` from `../api`.

## Routes

| Route | Component/behavior |
| --- | --- |
| `/` | Check-in/check-out status and totals |
| `/bookings` | Monthly workday table, entry editing, holidays, and event booking |
| `/calendar` | Compatibility redirect to `/bookings` |
| `/settings` | Working hours, break duration, working weekdays, theme, and data actions |

The bookings table is the canonical history UI. Do not create a separate calendar editing implementation; extend the table and its dialogs.

## Bookings data flow

For the selected month, `BookingsComponent` loads three resources concurrently:

- `/api/workdays/{yyyy-MM}` for calculated daily rows;
- `/api/events?from=...&to=...` for events overlapping the month;
- `/api/holidays/{year}` for public holidays.

Each row can open the time-entry dialog. Events can be created from the table toolbar or a specific date, and existing event badges open the event editor. Multi-day ranges render on every included row with a `Day n of m` label.

Event badges deliberately distinguish their types:

- vacation: blue with `lucideUmbrella`;
- sickness: red with `lucideHeartPulse`;
- other: violet with `lucideSparkles`;
- public holidays: a flag badge.

Events are always sent as all-day values. The JSON property is `allDay`; keep it non-null and aligned with the Kotlin `EventDto` contract.

## State and API conventions

- Keep transport interfaces in `core/models/worklog.model.ts` and conversions in the relevant service.
- API date-only fields are formatted as `yyyy-MM-dd`; time entries use local ISO date-time strings.
- Use `date-fns` for parsing, formatting, month boundaries, and inclusive event-range display.
- The status and holiday responses are validated with Zod. Extend runtime schemas when those external contracts change.
- Month navigation guards against stale asynchronous responses by checking the active month before applying results.
- API calls use `firstValueFrom` to expose promise-based methods to components.

## Source layout

```text
src/app/
├── core/
│   ├── models/             Domain and transport types
│   └── services/           HTTP and theme services
├── features/home/
│   ├── checkin/            Current status and clock action
│   ├── bookings/           Table plus entry/event dialogs
│   └── settings/           Work schedule and application settings
├── layout/                 Shell and navigation
└── shared/                 Reusable components, providers, and utilities
```

Use the `@/*` TypeScript alias for imports rooted at `src/app`. Preserve strict templates, keyboard navigation, semantic controls, visible focus, and WCAG AA contrast when changing the UI.

## Production image

`web/Dockerfile` performs a production Angular build with Node 24, copies `dist/client/browser` into Nginx, and configures SPA fallback to `index.html`. API routing is not handled by that Nginx container; Traefik supplies `/api` routing in the integrated deployment.
