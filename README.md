# clock-in

Clock In is a booking and worklog product for teams who need a simple way to plan time, track activity, and manage user access in one place.

This README focuses on the product itself. Technical implementation details are intentionally kept short here.

## Product at a glance

Clock In combines three core experiences:

- Booking and calendar workflows for planning work.
- Worklog and overview views for tracking what happened.
- Identity and account management so access stays controlled.

The result is one connected experience instead of separate tools for auth, scheduling, and activity tracking.

## Main product areas

### Authentication and account management

- Secure sign in and session handling.
- Password reset and account recovery flows.
- User profile and account-level settings.

### Booking and calendar

- Booking-focused user flows.
- Calendar-based visibility.
- A shared view of planned work.

### Worklog and overview

- Capture and review recorded work.
- Overview pages for quick status checks.
- Admin and settings capabilities to support operations.

### Notifications and email

- Transactional email support for user-facing flows.
- Template-based messaging for consistent communication.

## Who this is for

Clock In is designed for small to mid-sized teams that need:

- A lightweight internal platform.
- Clear ownership of user access and scheduling.
- A product they can host and evolve themselves.

## Repository overview

This repository contains everything required to deliver the product:

- Frontend application.
- Backend services.
- Local container configuration.

For contributors or operators who need implementation details, start with the component READMEs:

- `api/README.md`
- `web/README.md`

## Quick start

To run the integrated stack locally, use the root compose file:

```bash
docker compose up -d
```

After startup, open the app through the local gateway on port 3000.

The Compose defaults are deliberately obvious local-development credentials. Set
`POSTGRES_PASSWORD` and `APP_JWT_SECRET` to strong, operator-managed secrets before
using the stack outside a local machine.

## License

This repository is licensed under the GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later).
See the LICENSE file for the full license text.
