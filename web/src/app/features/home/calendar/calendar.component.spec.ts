import { ComponentFixture, TestBed } from '@angular/core/testing';
import { addDays, format, startOfWeek } from 'date-fns';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Event, Holiday } from '@/core/models/worklog.model';
import { EventService } from '@/core/services/event.service';
import { ZardDialogService } from '@/shared/components/dialog';
import { BookingEventDialogComponent } from '../bookings/booking-event-dialog/booking-event-dialog.component';
import { CalendarComponent } from './calendar.component';

describe('CalendarComponent', () => {
  const eventService = {
    getEvents: vi.fn(),
    getHolidays: vi.fn(),
    createEvent: vi.fn(),
    updateEvent: vi.fn(),
    deleteEvent: vi.fn(),
  };

  const dialogService = {
    create: vi.fn(),
  };

  const leave: Event = {
    id: 10,
    title: 'Summer Vacation',
    type: 'vacation',
    status: 'new',
    start: new Date(2026, 8, 7), // 2026-09-07 (Monday)
    end: new Date(2026, 8, 12), // 2026-09-12 (Saturday)
    allDay: true,
  };

  const doctor: Event = {
    id: 11,
    title: 'Doctor',
    type: 'sick',
    status: 'new',
    start: new Date(2026, 8, 8), // 2026-09-08 (Tuesday)
    end: new Date(2026, 8, 8),
    allDay: true,
  };

  const crossWeekEvent: Event = {
    id: 12,
    title: 'Conference',
    type: 'other',
    status: 'new',
    start: new Date(2026, 8, 25), // 2026-09-25 (Friday)
    end: new Date(2026, 8, 29), // 2026-09-29 (Tuesday)
    allDay: true,
  };

  const germanUnityHoliday: Holiday = {
    date: '2026-10-03',
    name: 'Tag der deutschen Einheit',
    allStates: true,
  };

  let fixture: ComponentFixture<CalendarComponent>;
  let component: CalendarComponent;

  beforeEach(async () => {
    vi.clearAllMocks();
    eventService.getEvents.mockResolvedValue([
      leave,
      doctor,
      crossWeekEvent,
    ]);
    eventService.getHolidays.mockResolvedValue([germanUnityHoliday]);

    await TestBed.configureTestingModule({
      imports: [CalendarComponent],
      providers: [
        { provide: EventService, useValue: eventService },
        { provide: ZardDialogService, useValue: dialogService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CalendarComponent);
    component = fixture.componentInstance;
  });

  it('loads events and holidays for the fixed 42-day calendar range on init', async () => {
    component.currentMonth.set(new Date(2026, 8, 1)); // September 2026
    await component.loadCalendarData();

    const expectedStart = startOfWeek(new Date(2026, 8, 1), {
      weekStartsOn: 1,
    });
    const expectedEnd = addDays(expectedStart, 41);

    expect(eventService.getEvents).toHaveBeenCalledWith({
      from: expectedStart,
      to: expectedEnd,
    });
    expect(eventService.getHolidays).toHaveBeenCalled();
    expect(component.events()).toEqual([leave, doctor, crossWeekEvent]);
    expect(component.holidays()).toEqual([germanUnityHoliday]);
    expect(component.weeks().length).toBe(6);
    expect(component.weeks()[0].days.length).toBe(7);
  });

  it('generates correct multi-day event spans and tracks', async () => {
    component.currentMonth.set(new Date(2026, 8, 1));
    await component.loadCalendarData();

    // In September 2026:
    // Week 0: Aug 31 - Sep 6
    // Week 1: Sep 7 (Mon) - Sep 13 (Sun)
    const week1 = component.weeks()[1];
    expect(format(week1.days[0].date, 'yyyy-MM-dd')).toBe('2026-09-07');

    const vacationSegment = week1.segments.find(
      (s) => s.event?.id === leave.id,
    );
    expect(vacationSegment).toBeDefined();
    expect(vacationSegment?.startCol).toBe(1); // Monday
    expect(vacationSegment?.span).toBe(6); // Mon through Sat (6 days)
    expect(vacationSegment?.track).toBe(0);

    // Doctor event is on Tue Sep 8 (col 2). Since Vacation occupies col 1-6 on track 0,
    // Doctor should be assigned to track 1!
    const doctorSegment = week1.segments.find(
      (s) => s.event?.id === doctor.id,
    );
    expect(doctorSegment).toBeDefined();
    expect(doctorSegment?.startCol).toBe(2);
    expect(doctorSegment?.span).toBe(1);
    expect(doctorSegment?.track).toBe(1);
  });

  it('splits cross-week events across week rows with continuation flags', async () => {
    component.currentMonth.set(new Date(2026, 8, 1));
    await component.loadCalendarData();

    // Conference: 2026-09-25 (Fri) to 2026-09-29 (Tue)
    // Week 3: Sep 21 - Sep 27 (Fri is day 4 / col 5)
    // Week 4: Sep 28 - Oct 4 (Mon is day 0 / col 1, Tue is day 1 / col 2)
    const week3 = component.weeks()[3];
    const week4 = component.weeks()[4];

    const segWeek3 = week3.segments.find(
      (s) => s.event?.id === crossWeekEvent.id,
    );
    expect(segWeek3).toBeDefined();
    expect(segWeek3?.startCol).toBe(5); // Friday
    expect(segWeek3?.span).toBe(3); // Fri, Sat, Sun
    expect(segWeek3?.continuesBefore).toBe(false);
    expect(segWeek3?.continuesAfter).toBe(true);

    const segWeek4 = week4.segments.find(
      (s) => s.event?.id === crossWeekEvent.id,
    );
    expect(segWeek4).toBeDefined();
    expect(segWeek4?.startCol).toBe(1); // Monday
    expect(segWeek4?.span).toBe(2); // Mon, Tue
    expect(segWeek4?.continuesBefore).toBe(true);
    expect(segWeek4?.continuesAfter).toBe(false);
  });

  it('renders holidays on the correct day with flag icon', async () => {
    component.currentMonth.set(new Date(2026, 8, 1));
    await component.loadCalendarData();

    // German Unity Day: 2026-10-03 (Saturday) -> In Week 4 (Sep 28 - Oct 4)
    const week4 = component.weeks()[4];
    const holidaySegment = week4.segments.find(
      (s) => s.kind === 'holiday' && s.title === 'Tag der deutschen Einheit',
    );
    expect(holidaySegment).toBeDefined();
    expect(holidaySegment?.startCol).toBe(6); // Saturday
    expect(holidaySegment?.span).toBe(1);
    expect(holidaySegment?.iconName).toBe('lucideFlag');
  });

  it('navigates between months and today', async () => {
    component.currentMonth.set(new Date(2026, 8, 1)); // Sep 2026

    await component.navigate('next');
    expect(format(component.currentMonth(), 'yyyy-MM')).toBe('2026-10');

    await component.navigate('prev');
    expect(format(component.currentMonth(), 'yyyy-MM')).toBe('2026-09');

    await component.navigate('today');
    expect(format(component.currentMonth(), 'yyyy-MM')).toBe(
      format(new Date(), 'yyyy-MM'),
    );
  });

  it('opens BookingEventDialog when clicking on a day cell', () => {
    const clickedDate = new Date(2026, 8, 15);
    component.openAddEvent(clickedDate);

    expect(dialogService.create).toHaveBeenCalledWith(
      expect.objectContaining({
        zTitle: 'Book event',
        zContent: BookingEventDialogComponent,
        zData: expect.objectContaining({
          initialDate: clickedDate,
          event: undefined,
        }),
      }),
    );
  });

  it('opens BookingEventDialog in edit mode when clicking on an event', () => {
    component.openEditEvent(leave);

    expect(dialogService.create).toHaveBeenCalledWith(
      expect.objectContaining({
        zTitle: 'Edit event',
        zContent: BookingEventDialogComponent,
        zData: expect.objectContaining({
          initialDate: leave.start,
          event: leave,
        }),
      }),
    );
  });

  it('provides matching colors and icons for event types', () => {
    expect(component.eventColorClass('vacation')).toContain('bg-blue-600');
    expect(component.eventColorClass('sick')).toContain('bg-red-600');
    expect(component.eventColorClass('other')).toContain('bg-indigo-600');

    expect(component.eventIconName('vacation')).toBe('lucideUmbrella');
    expect(component.eventIconName('sick')).toBe('lucideHeartPulse');
    expect(component.eventIconName('other')).toBe('lucideSparkles');
  });
});
