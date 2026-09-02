import {
  Component,
  signal,
  computed,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon } from '@ng-icons/core';
import { FullCalendarModule } from '@fullcalendar/angular';
import {
  CalendarOptions,
  DateSelectArg,
  EventClickArg,
  EventSourceInput,
} from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { FullCalendarComponent } from '@fullcalendar/angular';
import { format, add } from 'date-fns';
import { EventService } from '@/core/services/event.service';
import { Event } from '@/core/models/worklog.model';
import { ZardDialogService } from '@/shared/components/dialog';
import { ZardDialogRef } from '@/shared/components/dialog';
import { ZardButtonComponent } from '@/shared/components/button';
import {
  CalendarEventDialogComponent,
  CalendarEventDialogData,
} from './calendar-event-dialog/calendar-event-dialog.component';

@Component({
  selector: 'app-calendar-page',
  imports: [CommonModule, FullCalendarModule, ZardButtonComponent, NgIcon],
  templateUrl: './calendar-page.component.html',
  styles: [
    `
      :host ::ng-deep .fc {
        color: var(--foreground);
      }
      :host ::ng-deep .fc-day-today {
        background-color: color-mix(
          in oklch,
          #8b5cf6 28%,
          transparent
        ) !important;
      }
      :host ::ng-deep .fc-daygrid-day-number {
        color: var(--foreground);
      }
      :host ::ng-deep .fc-col-header-cell {
        background-color: var(--muted);
      }
      :host ::ng-deep .fc-button {
        display: none;
      }
    `,
  ],
})
export class CalendarPageComponent implements OnInit {
  @ViewChild('calendar') calendarComponent?: FullCalendarComponent;

  private eventService = inject(EventService);
  private dialogService = inject(ZardDialogService);
  private dialogRef: ZardDialogRef<unknown> | null = null;

  currentDate = signal(new Date());
  events = signal<Event[]>([]);
  holidays = signal<Event[]>([]);
  isLoading = signal(true);
  selectedEvent = signal<Event | null>(null);

  readonly currentMonthLabel = computed(() =>
    format(this.currentDate(), 'MMMM yyyy'),
  );

  readonly calendarOptions = computed<CalendarOptions>(() => ({
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    initialDate: this.currentDate(),
    headerToolbar: false,
    selectable: true,
    selectMirror: true,
    dayMaxEvents: true,
    weekends: true,
    firstDay: 1,
    height: 'auto',
    select: (info: DateSelectArg) => this.handleDateSelect(info),
    eventClick: (info: EventClickArg) => this.handleEventClick(info),
    eventSources: this.buildEventSources(),
  }));

  async ngOnInit(): Promise<void> {
    await this.loadData();
  }

  private async loadData(): Promise<void> {
    try {
      this.isLoading.set(true);
      const [eventsData, holidaysData] = await Promise.all([
        this.eventService.getEvents(),
        this.eventService.getHolidays(this.currentDate()),
      ]);

      const transformedEvents = eventsData.map((e) => ({
        ...e,
        end: add(e.end, { days: 1 }),
      }));

      const transformedHolidays: Event[] = holidaysData.map((h) => ({
        title: h.name,
        type: 'none' as const,
        status: 'new' as const,
        start: new Date(h.date),
        end: new Date(h.date),
        allDay: true,
      }));

      this.events.set(transformedEvents);
      this.holidays.set(transformedHolidays);
    } catch (error) {
      console.error('Error loading calendar data:', error);
    } finally {
      this.isLoading.set(false);
    }
  }

  private buildEventSources(): EventSourceInput[] {
    return [
      {
        id: 'events',
        events: this.events().map((e) => ({
          id: e.id?.toString(),
          title: e.title,
          start: e.start,
          end: e.end,
          allDay: e.allDay,
          extendedProps: { type: e.type, status: e.status },
          backgroundColor:
            e.type === 'vacation'
              ? '#3b82f6'
              : e.type === 'sick'
                ? '#ef4444'
                : '#6366f1',
          borderColor:
            e.type === 'vacation'
              ? '#2563eb'
              : e.type === 'sick'
                ? '#dc2626'
                : '#4f46e5',
          textColor: 'white',
        })),
        color: 'blue',
      },
      {
        id: 'holidays',
        events: this.holidays().map((h) => ({
          title: h.title,
          start: h.start,
          end: h.end,
          allDay: true,
          backgroundColor: '#ef4444',
          borderColor: '#dc2626',
          textColor: 'white',
        })),
        color: 'red',
      },
    ];
  }

  handleDateSelect(info: DateSelectArg): void {
    const newEvent: Event = {
      title: '',
      type: 'none',
      status: 'new',
      start: info.start,
      end: info.end || new Date(info.start.getTime() + 24 * 60 * 60 * 1000),
      allDay: info.allDay,
    };
    this.selectedEvent.set(newEvent);
    this.openEventDialog(newEvent, { title: '', type: 'none' });
  }

  handleEventClick(info: EventClickArg): void {
    if (info.event.source?.id === 'holidays') return;
    if (info.event.start) {
      const event: Event = {
        id: info.event.id ? Number(info.event.id) : undefined,
        type: (info.event.extendedProps['type'] as Event['type']) || 'none',
        title: info.event.title || '',
        status: 'new',
        start: info.event.start,
        end: info.event.end || info.event.start,
        allDay: info.event.allDay,
      };
      this.selectedEvent.set(event);
      this.openEventDialog(event, { title: event.title, type: event.type });
    }
  }

  openAddEvent(): void {
    const start = new Date();
    start.setHours(9, 0, 0, 0);
    const end = new Date(start);
    end.setHours(17, 0, 0, 0);
    const event: Event = {
      title: '',
      type: 'none',
      status: 'new',
      start,
      end,
      allDay: false,
    };
    this.selectedEvent.set(event);
    this.openEventDialog(event, { title: '', type: 'none' });
  }

  private openEventDialog(
    event: Event,
    initialForm: { title: string; type: Event['type'] },
  ): void {
    const data: CalendarEventDialogData = {
      event,
      initialForm,
      onSave: (title, type) => this.saveEvent(title, type),
      onDelete: () => this.deleteEvent(),
    };
    this.dialogService.create({
      zTitle: event.id ? 'Edit Event' : 'Add Event',
      zContent: CalendarEventDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '520px',
    });
  }

  async navigate(direction: 'prev' | 'next' | 'today'): Promise<void> {
    const api = this.calendarComponent?.getApi();
    if (!api) return;

    let newDate: Date;
    switch (direction) {
      case 'prev':
        api.prev();
        newDate = add(this.currentDate(), { months: -1 });
        break;
      case 'next':
        api.next();
        newDate = add(this.currentDate(), { months: 1 });
        break;
      case 'today':
        api.today();
        newDate = new Date();
        break;
    }

    const prevYear = this.currentDate().getFullYear();
    this.currentDate.set(newDate!);

    if (newDate!.getFullYear() !== prevYear) {
      try {
        const holidaysData = await this.eventService.getHolidays(newDate!);
        this.holidays.set(
          holidaysData.map((h) => ({
            title: h.name,
            type: 'none' as const,
            status: 'new' as const,
            start: new Date(h.date),
            end: new Date(h.date),
            allDay: true,
          })),
        );
      } catch (error) {
        console.error('Error loading holidays:', error);
      }
    }
  }

  async saveEvent(title: string, type: Event['type']): Promise<void> {
    const event = this.selectedEvent();
    if (!event) return;

    try {
      const eventToSave = { ...event, title, type };
      if (eventToSave.id) {
        const updated = await this.eventService.updateEvent(eventToSave);
        updated.end = add(updated.end, { days: 1 });
        this.events.update((prev) =>
          prev.map((e) => (e.id === updated.id ? updated : e)),
        );
      } else {
        const created = await this.eventService.createEvent(eventToSave);
        created.end = add(created.end, { days: 1 });
        this.events.update((prev) => [...prev, created]);
      }
      this.selectedEvent.set(null);
    } catch (error) {
      console.error('Error saving event:', error);
    }
  }

  async deleteEvent(): Promise<void> {
    const event = this.selectedEvent();
    if (!event?.id) return;
    try {
      await this.eventService.deleteEvent(event.id);
      this.events.update((prev) => prev.filter((e) => e.id !== event.id));
      this.selectedEvent.set(null);
    } catch (error) {
      console.error('Error deleting event:', error);
    }
  }

  closeDialog(): void {
    this.dialogRef?.close();
    this.dialogRef = null;
    this.selectedEvent.set(null);
  }
}
