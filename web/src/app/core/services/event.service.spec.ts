import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { Event } from '../models/worklog.model';
import { EventService } from './event.service';

describe('EventService', () => {
  it('uses inclusive LocalDate values rather than date-time strings', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(EventService);
    const http = TestBed.inject(HttpTestingController);
    const event: Omit<Event, 'id'> = {
      title: 'Annual leave',
      type: 'vacation',
      status: 'new',
      start: new Date(2026, 8, 3),
      end: new Date(2026, 8, 5),
      allDay: true,
    };

    const created = service.createEvent(event);
    const request = http.expectOne('/api/events');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      title: 'Annual leave',
      type: 'vacation',
      status: 'new',
      start: '2026-09-03',
      end: '2026-09-05',
      allDay: true,
    });
    request.flush({ id: 7, ...request.request.body });

    await expect(created).resolves.toMatchObject({
      id: 7,
      allDay: true,
    });
    http.verify();
  });

  it('supports backend other and approved event values', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(EventService);
    const http = TestBed.inject(HttpTestingController);

    const events = service.getEvents();
    http.expectOne('/api/events').flush([
      {
        id: 9,
        title: 'Training',
        type: 'other',
        status: 'approved',
        start: '2026-10-01',
        end: '2026-10-01',
      },
    ]);

    await expect(events).resolves.toMatchObject([
      { type: 'other', status: 'approved' },
    ]);
    http.verify();
  });

  it('requests events overlapping the selected inclusive month', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(EventService);
    const http = TestBed.inject(HttpTestingController);

    const events = service.getEvents({
      from: new Date(2026, 8, 1),
      to: new Date(2026, 8, 30),
    });
    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/events' &&
        candidate.params.get('from') === '2026-09-01' &&
        candidate.params.get('to') === '2026-09-30',
    );
    request.flush([]);

    await expect(events).resolves.toEqual([]);
    http.verify();
  });
});
