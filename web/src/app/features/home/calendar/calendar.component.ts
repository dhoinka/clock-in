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
  addDays,
  addMonths,
  differenceInCalendarDays,
  format,
  getDate,
  isSameDay,
  isSameMonth,
  startOfMonth,
  startOfWeek,
} from 'date-fns';

import { Event } from '@/core/models/worklog.model';
import { EventService } from '@/core/services/event.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogService } from '@/shared/components/dialog';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import {
  BookingEventDialogComponent,
  BookingEventDialogData,
} from '../bookings/booking-event-dialog/booking-event-dialog.component';

export interface CalendarDay {
  readonly date: Date;
  readonly dateKey: string;
  readonly dayNumber: number;
  readonly isCurrentMonth: boolean;
  readonly isToday: boolean;
}

export type EventIconName =
  'lucideUmbrella' | 'lucideHeartPulse' | 'lucideSparkles' | 'lucideFlag';

export interface CalendarSegment {
  readonly id: string;
  readonly kind: 'event' | 'holiday';
  readonly title: string;
  readonly type?: Event['type'];
  readonly event?: Event;
  readonly startCol: number;
  readonly span: number;
  readonly track: number;
  readonly continuesBefore: boolean;
  readonly continuesAfter: boolean;
  readonly colorClass: string;
  readonly iconName?: EventIconName;
}

export interface CalendarWeek {
  readonly days: CalendarDay[];
  readonly segments: CalendarSegment[];
  readonly maxTracks: number;
}

@Component({
  selector: 'app-calendar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ZardButtonComponent, ZardSkeletonComponent, NgIcon],
  templateUrl: './calendar.component.html',
})
export class CalendarComponent implements OnInit {
  private readonly eventService = inject(EventService);
  private readonly dialogService = inject(ZardDialogService);

  readonly format = format;
  readonly currentMonth = signal<Date>(startOfMonth(new Date()));
  readonly events = signal<Event[]>([]);
  readonly loading = signal<boolean>(true);
  readonly loadError = signal<string>('');

  readonly selectedMonthLabel = computed(() =>
    format(this.currentMonth(), 'MMMM yyyy'),
  );

  readonly calendarRange = computed(() => {
    const month = this.currentMonth();
    const monthStart = startOfMonth(month);
    const start = startOfWeek(monthStart, { weekStartsOn: 1 });
    const end = addDays(start, 41); // Fixed 6 weeks (42 days)
    return { start, end };
  });

  readonly weeks = computed<CalendarWeek[]>(() => {
    const { start: gridStart } = this.calendarRange();
    const currentMonthDate = this.currentMonth();
    const today = new Date();
    const allEvents = this.events();

    const weeksList: CalendarWeek[] = [];

    for (let w = 0; w < 6; w++) {
      const weekStart = addDays(gridStart, w * 7);
      const weekEnd = addDays(weekStart, 6);

      const days: CalendarDay[] = [];
      for (let d = 0; d < 7; d++) {
        const date = addDays(weekStart, d);
        days.push({
          date,
          dateKey: format(date, 'yyyy-MM-dd'),
          dayNumber: getDate(date),
          isCurrentMonth: isSameMonth(date, currentMonthDate),
          isToday: isSameDay(date, today),
        });
      }

      interface RawSegment {
        id: string;
        kind: 'event' | 'holiday';
        title: string;
        type?: Event['type'];
        event?: Event;
        startIndex: number;
        endIndex: number;
        span: number;
        continuesBefore: boolean;
        continuesAfter: boolean;
        colorClass: string;
        iconName?: EventIconName;
      }

      const rawSegments: RawSegment[] = [];

      for (const event of allEvents) {
        const eventStartKey = format(event.start, 'yyyy-MM-dd');
        const eventEndKey = format(event.end, 'yyyy-MM-dd');
        const weekStartKey = format(weekStart, 'yyyy-MM-dd');
        const weekEndKey = format(weekEnd, 'yyyy-MM-dd');

        if (eventStartKey <= weekEndKey && eventEndKey >= weekStartKey) {
          const startDiff = differenceInCalendarDays(event.start, weekStart);
          const endDiff = differenceInCalendarDays(event.end, weekStart);
          const startIndex = Math.max(0, startDiff);
          const endIndex = Math.min(6, endDiff);
          const span = endIndex - startIndex + 1;

          rawSegments.push({
            id: `event-${event.id}-${w}`,
            kind: event.type === 'holiday' ? 'holiday' : 'event',
            title: event.title,
            type: event.type,
            event,
            startIndex,
            endIndex,
            span,
            continuesBefore: startDiff < 0,
            continuesAfter: endDiff > 6,
            colorClass: this.eventColorClass(event.type),
            iconName: this.eventIconName(event.type),
          });
        }
      }

      rawSegments.sort((a, b) => {
        if (a.startIndex !== b.startIndex) return a.startIndex - b.startIndex;
        if (b.span !== a.span) return b.span - a.span;
        if (a.kind !== b.kind) return a.kind === 'event' ? -1 : 1;
        return a.title.localeCompare(b.title);
      });

      const tracks: boolean[][] = [];
      const segments: CalendarSegment[] = [];

      for (const seg of rawSegments) {
        let track = 0;
        while (true) {
          if (!tracks[track]) {
            tracks[track] = [];
            break;
          }
          let overlaps = false;
          for (let i = seg.startIndex; i <= seg.endIndex; i++) {
            if (tracks[track][i]) {
              overlaps = true;
              break;
            }
          }
          if (!overlaps) break;
          track++;
        }

        for (let i = seg.startIndex; i <= seg.endIndex; i++) {
          tracks[track][i] = true;
        }

        segments.push({
          id: seg.id,
          kind: seg.kind,
          title: seg.title,
          type: seg.type,
          event: seg.event,
          startCol: seg.startIndex + 1,
          span: seg.span,
          track,
          continuesBefore: seg.continuesBefore,
          continuesAfter: seg.continuesAfter,
          colorClass: seg.colorClass,
          iconName: seg.iconName,
        });
      }

      weeksList.push({
        days,
        segments,
        maxTracks: tracks.length,
      });
    }

    return weeksList;
  });

  async ngOnInit(): Promise<void> {
    await this.loadCalendarData();
  }

  async navigate(direction: 'prev' | 'next' | 'today'): Promise<void> {
    const current = this.currentMonth();
    const nextMonth =
      direction === 'today'
        ? startOfMonth(new Date())
        : addMonths(current, direction === 'next' ? 1 : -1);
    this.currentMonth.set(startOfMonth(nextMonth));
    await this.loadCalendarData();
  }

  async loadCalendarData(): Promise<void> {
    const { start, end } = this.calendarRange();
    const monthKey = format(this.currentMonth(), 'yyyy-MM');
    this.loading.set(true);
    this.loadError.set('');

    try {
      const events = await this.eventService.getEvents({
        from: start,
        to: end,
      });

      if (monthKey !== format(this.currentMonth(), 'yyyy-MM')) {
        return;
      }

      this.events.set(events);
    } catch (error) {
      console.error('Failed to load calendar data:', error);
      if (monthKey === format(this.currentMonth(), 'yyyy-MM')) {
        this.loadError.set(
          'Calendar data could not be loaded. Please try again.',
        );
      }
    } finally {
      if (monthKey === format(this.currentMonth(), 'yyyy-MM')) {
        this.loading.set(false);
      }
    }
  }

  openAddEvent(date?: Date): void {
    const today = new Date();
    const initialDate =
      date ??
      (isSameMonth(today, this.currentMonth()) ? today : this.currentMonth());
    this.openEventDialog(initialDate);
  }

  openEditEvent(event: Event): void {
    this.openEventDialog(event.start, event);
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
        await this.loadCalendarData();
      },
      onDelete: event?.id
        ? async () => {
            await this.eventService.deleteEvent(event.id!);
            await this.loadCalendarData();
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

  eventColorClass(type?: Event['type']): string {
    switch (type) {
      case 'vacation':
        return 'bg-blue-600 dark:bg-blue-600 hover:bg-blue-500 text-white';
      case 'sick':
        return 'bg-red-600 dark:bg-red-600 hover:bg-red-500 text-white';
      case 'holiday':
        return 'bg-red-500/90 dark:bg-red-600 hover:bg-red-500 text-white';
      default:
        return 'bg-indigo-600 dark:bg-indigo-600 hover:bg-indigo-500 text-white';
    }
  }

  eventIconName(type?: Event['type']): EventIconName {
    switch (type) {
      case 'vacation':
        return 'lucideUmbrella';
      case 'sick':
        return 'lucideHeartPulse';
      case 'holiday':
        return 'lucideFlag';
      default:
        return 'lucideSparkles';
    }
  }
}
