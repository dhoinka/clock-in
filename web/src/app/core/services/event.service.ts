import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { format, parseISO } from 'date-fns';
import { Event, Holiday, holidaySchema } from '../models/worklog.model';
import { z } from 'zod';

interface EventDto {
  id?: number;
  title: string;
  type: string;
  status: string;
  start: string;
  end: string;
  allDay?: boolean;
  user?: string;
}

@Injectable({ providedIn: 'root' })
export class EventService {
  private http = inject(HttpClient);

  async getEvents(): Promise<Event[]> {
    const dtos = await firstValueFrom(this.http.get<EventDto[]>('/api/events'));
    return dtos.map((dto) => this.convertDtoToEvent(dto));
  }

  async createEvent(event: Omit<Event, 'id'>): Promise<Event> {
    const dto = await firstValueFrom(
      this.http.post<EventDto>('/api/events', this.convertEventToDto(event)),
    );
    return this.convertDtoToEvent(dto);
  }

  async updateEvent(event: Event): Promise<Event> {
    const dto = await firstValueFrom(
      this.http.put<EventDto>(
        `/api/events/${event.id}`,
        this.convertEventToDto(event),
      ),
    );
    return this.convertDtoToEvent(dto);
  }

  async deleteEvent(eventId: number): Promise<void> {
    await firstValueFrom(this.http.delete(`/api/events/${eventId}`));
  }

  async getHolidays(date: Date): Promise<Holiday[]> {
    const year = date.getFullYear();
    const response = await firstValueFrom(
      this.http.get(`/api/holidays/${year}`),
    );
    return holidaySchema.array().parse(response);
  }

  private convertDtoToEvent(dto: EventDto): Event {
    return {
      id: dto.id,
      title: dto.title,
      type: dto.type as Event['type'],
      status: dto.status as Event['status'],
      start: parseISO(dto.start),
      end: parseISO(dto.end),
      allDay: dto.allDay ?? true,
      user: dto.user,
    };
  }

  private convertEventToDto(
    event: Omit<Event, 'id'> & { id?: number },
  ): object {
    return {
      title: event.title,
      type: event.type,
      status: event.status,
      start: format(event.start, "yyyy-MM-dd'T'HH:mm:ss"),
      end: format(event.end, "yyyy-MM-dd'T'HH:mm:ss"),
      allDay: event.allDay,
      user: event.user,
    };
  }
}
