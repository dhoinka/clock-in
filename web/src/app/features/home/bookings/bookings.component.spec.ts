import { ComponentFixture, TestBed } from '@angular/core/testing';
import { endOfMonth } from 'date-fns';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Event, Holiday, Workday } from '@/core/models/worklog.model';
import { EventService } from '@/core/services/event.service';
import { WorklogService } from '@/core/services/worklog.service';
import { ZardDialogService } from '@/shared/components/dialog';
import { BookingsComponent } from './bookings.component';

describe('BookingsComponent', () => {
  const worklogService = {
    getWorkdaysByMonth: vi.fn(),
  };
  const eventService = {
    getEvents: vi.fn(),
    getHolidays: vi.fn(),
  };

  const workday: Workday = {
    date: new Date(2026, 8, 4),
    entries: [],
    gross: '00:00',
    balance: '00:00',
    workday: true,
  };

  const leave: Event = {
    id: 3,
    title: 'Annual leave',
    type: 'vacation',
    status: 'approved',
    start: new Date(2026, 8, 3),
    end: new Date(2026, 8, 5),
    allDay: true,
  };

  let fixture: ComponentFixture<BookingsComponent>;
  let component: BookingsComponent;

  beforeEach(async () => {
    vi.clearAllMocks();
    worklogService.getWorkdaysByMonth.mockResolvedValue([workday]);
    eventService.getEvents.mockResolvedValue([leave]);
    eventService.getHolidays.mockResolvedValue([]);
    await TestBed.configureTestingModule({
      imports: [BookingsComponent],
      providers: [
        { provide: WorklogService, useValue: worklogService },
        { provide: EventService, useValue: eventService },
        { provide: ZardDialogService, useValue: { create: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BookingsComponent);
    component = fixture.componentInstance;
  });

  it('loads workdays, events, and holidays together and keeps overlapping ranges', async () => {
    component.currentMonth.set(new Date(2026, 8, 1));
    await component.loadBookings();

    expect(worklogService.getWorkdaysByMonth).toHaveBeenCalledWith('2026-09');
    expect(eventService.getEvents).toHaveBeenCalledWith({
      from: new Date(2026, 8, 1),
      to: endOfMonth(new Date(2026, 8, 1)),
    });
    expect(eventService.getHolidays).toHaveBeenCalledOnce();
    expect(component.bookingsMonth()).toEqual([workday]);
    expect(component.events()).toEqual([leave]);
  });

  it('shows inclusive multi-day event progress and read-only holidays on each row', () => {
    const holiday: Holiday = {
      date: '2026-09-04',
      name: 'Regional holiday',
      allStates: false,
    };
    component.events.set([leave]);
    component.holidays.set([holiday]);

    expect(component.displayEvents(workday)).toEqual([
      {
        key: 'event-3',
        kind: 'event',
        title: 'Annual leave',
        event: leave,
        dayLabel: 'Day 2 of 3',
      },
      {
        key: 'holiday-2026-09-04-Regional holiday',
        kind: 'holiday',
        title: 'Regional holiday',
      },
    ]);
  });

  it('uses a distinct icon for each event type', () => {
    expect(component.eventIconName(leave)).toBe('lucideUmbrella');
    expect(component.eventIconName({ ...leave, type: 'sick' })).toBe(
      'lucideHeartPulse',
    );
    expect(component.eventIconName({ ...leave, type: 'other' })).toBe(
      'lucideSparkles',
    );
  });
});
