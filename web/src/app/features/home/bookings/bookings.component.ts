import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon } from '@ng-icons/core';
import {
  addMonths,
  differenceInCalendarDays,
  endOfMonth,
  format,
  isSameMonth,
  startOfMonth,
} from 'date-fns';

import { Event, Holiday, Workday } from '@/core/models/worklog.model';
import { EventService } from '@/core/services/event.service';
import { WorklogService } from '@/core/services/worklog.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogService } from '@/shared/components/dialog';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardTableImports } from '@/shared/components/table';
import {
  BookingEditDialogComponent,
  BookingEditDialogData,
} from './booking-edit-dialog/booking-edit-dialog.component';
import {
  BookingEventDialogComponent,
  BookingEventDialogData,
} from './booking-event-dialog/booking-event-dialog.component';

interface DisplayEvent {
  readonly key: string;
  readonly kind: 'event' | 'holiday';
  readonly title: string;
  readonly event?: Event;
  readonly dayLabel?: string;
}

type EventIconName = 'lucideHeartPulse' | 'lucideSparkles' | 'lucideUmbrella';

@Component({
  selector: 'app-bookings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ZardButtonComponent,
    ZardSkeletonComponent,
    ...ZardTableImports,
    NgIcon,
  ],
  templateUrl: './bookings.component.html',
})
export class BookingsComponent implements OnInit {
  private readonly worklogService = inject(WorklogService);
  private readonly eventService = inject(EventService);
  private readonly dialogService = inject(ZardDialogService);

  readonly skeletonRows = Array.from({ length: 31 }, (_, i) => i);
  readonly format = format;
  readonly currentMonth = signal(startOfMonth(new Date()));
  readonly bookingsMonth = signal<Workday[]>([]);
  readonly events = signal<Event[]>([]);
  readonly holidays = signal<Holiday[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal('');

  readonly selectedMonth = computed(() =>
    format(this.currentMonth(), 'yyyy-MM'),
  );
  readonly selectedMonthLabel = computed(() =>
    format(this.currentMonth(), 'MMMM yyyy'),
  );

  async ngOnInit(): Promise<void> {
    await this.loadBookings();
  }

  async navigate(direction: 'prev' | 'next' | 'today'): Promise<void> {
    const current = this.currentMonth();
    const nextMonth =
      direction === 'today'
        ? startOfMonth(new Date())
        : addMonths(current, direction === 'next' ? 1 : -1);
    this.currentMonth.set(startOfMonth(nextMonth));
    await this.loadBookings();
  }

  async loadBookings(): Promise<void> {
    const month = this.currentMonth();
    const monthKey = format(month, 'yyyy-MM');
    this.loading.set(true);
    this.loadError.set('');
    try {
      const [workdays, events, holidays] = await Promise.all([
        this.worklogService.getWorkdaysByMonth(monthKey),
        this.eventService.getEvents({
          from: startOfMonth(month),
          to: endOfMonth(month),
        }),
        this.eventService.getHolidays(month),
      ]);

      // Ignore a slower response after the user has already moved months.
      if (monthKey !== this.selectedMonth()) {
        return;
      }
      this.bookingsMonth.set(workdays);
      this.events.set(events);
      this.holidays.set(holidays);
    } catch (error) {
      console.error('Failed to load bookings:', error);
      if (monthKey === this.selectedMonth()) {
        this.loadError.set('Bookings could not be loaded. Please try again.');
      }
    } finally {
      if (monthKey === this.selectedMonth()) {
        this.loading.set(false);
      }
    }
  }

  getRowClass(element: Workday): string {
    const isToday = this.dateKey(element.date) === this.dateKey(new Date());
    if (isToday) return 'bg-accent/100';
    if (!element.workday) return 'bg-muted/30';
    return '';
  }

  getStartTime(element: Workday): string {
    const entry = element.entries.find((item) => item.type !== 'correction');
    return entry?.start ? format(entry.start, 'HH:mm') : '';
  }

  getEndTime(element: Workday): string {
    const entries = element.entries.filter(
      (item) => item.type !== 'correction',
    );
    const entry = entries.at(-1);
    return entry?.end ? format(entry.end, 'HH:mm') : '';
  }

  displayEvents(row: Workday): DisplayEvent[] {
    const date = this.dateKey(row.date);
    const events = this.events()
      .filter((event) => this.isEventOnDate(event, date))
      .map((event) => ({
        key: `event-${event.id}`,
        kind: 'event' as const,
        title: event.title,
        event,
        dayLabel: this.eventDayLabel(event, row.date),
      }));
    const holidays = this.holidays()
      .filter((holiday) => holiday.date === date)
      .map((holiday) => ({
        key: `holiday-${holiday.date}-${holiday.name}`,
        kind: 'holiday' as const,
        title: holiday.name,
      }));
    return [...events, ...holidays];
  }

  eventBadgeClass(event: Event): string {
    switch (event.type) {
      case 'vacation':
        return 'border-blue-300 bg-blue-100 text-blue-900 dark:border-blue-800 dark:bg-blue-950 dark:text-blue-100';
      case 'sick':
        return 'border-red-300 bg-red-100 text-red-900 dark:border-red-800 dark:bg-red-950 dark:text-red-100';
      default:
        return 'border-violet-300 bg-violet-100 text-violet-900 dark:border-violet-800 dark:bg-violet-950 dark:text-violet-100';
    }
  }

  eventIconName(event: Event): EventIconName {
    switch (event.type) {
      case 'vacation':
        return 'lucideUmbrella';
      case 'sick':
        return 'lucideHeartPulse';
      default:
        return 'lucideSparkles';
    }
  }

  openEditDialog(row: Workday): void {
    const data: BookingEditDialogData = {
      row,
      onSaved: () => void this.loadBookings(),
    };
    this.dialogService.create({
      zTitle: 'Edit time entries',
      zContent: BookingEditDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '720px',
    });
  }

  openNewEvent(date?: Date): void {
    const today = new Date();
    const initialDate = isSameMonth(today, this.currentMonth())
      ? today
      : this.currentMonth();
    this.openEventDialog(date ?? initialDate);
  }

  openEventDialog(initialDate: Date, event?: Event): void {
    const data: BookingEventDialogData = {
      event,
      initialDate,
      onSave: async (eventToSave) => {
        if (eventToSave.id !== undefined) {
          await this.eventService.updateEvent(eventToSave);
        } else {
          await this.eventService.createEvent(eventToSave);
        }
        await this.loadBookings();
      },
      onDelete: event?.id
        ? async () => {
            await this.eventService.deleteEvent(event.id!);
            await this.loadBookings();
          }
        : undefined,
    };
    this.dialogService.create({
      zTitle: event?.id ? 'Edit event' : 'Book event',
      zContent: BookingEventDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '520px',
    });
  }

  private isEventOnDate(event: Event, date: string): boolean {
    return this.dateKey(event.start) <= date && date <= this.dateKey(event.end);
  }

  private eventDayLabel(event: Event, date: Date): string | undefined {
    const totalDays = differenceInCalendarDays(event.end, event.start) + 1;
    if (totalDays < 2) {
      return undefined;
    }
    const day = differenceInCalendarDays(date, event.start) + 1;
    return `Day ${day} of ${totalDays}`;
  }

  private dateKey(date: Date): string {
    return format(date, 'yyyy-MM-dd');
  }
}
