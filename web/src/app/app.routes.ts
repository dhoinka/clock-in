import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent,
      ),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/home/checkin/checkin.component').then(
            (m) => m.CheckinComponent,
          ),
      },
      {
        path: 'bookings',
        loadComponent: () =>
          import('./features/home/bookings/bookings.component').then(
            (m) => m.BookingsComponent,
          ),
      },
      {
        path: 'calendar',
        loadComponent: () =>
          import('./features/home/calendar/calendar-page.component').then(
            (m) => m.CalendarPageComponent,
          ),
      },
      {
        path: 'overview',
        loadComponent: () =>
          import('./features/home/overview/overview.component').then(
            (m) => m.OverviewComponent,
          ),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/home/settings/settings.component').then(
            (m) => m.SettingsComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
