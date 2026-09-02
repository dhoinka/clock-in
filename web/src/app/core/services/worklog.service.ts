import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { format, parseISO } from 'date-fns';
import {
  StatusResponse,
  Workday,
  DayDto,
  WorklogUpdateRequest,
  EntryUpdateRequest,
  Entry,
  EntryDto,
  Stat,
  statusResponseSchema,
} from '../models/worklog.model';

@Injectable({ providedIn: 'root' })
export class WorklogService {
  private http = inject(HttpClient);

  async getStatus(): Promise<StatusResponse> {
    const response = await firstValueFrom(this.http.get('/api/status'));
    return statusResponseSchema.parse(response);
  }

  async postStatus(): Promise<StatusResponse> {
    const response = await firstValueFrom(this.http.post('/api/status', {}));
    return statusResponseSchema.parse(response);
  }

  async getWorkdaysByMonth(month: string): Promise<Workday[]> {
    const dtos = await firstValueFrom(
      this.http.get<DayDto[]>(`/api/workdays/${month}`),
    );
    return this.convertDayDtosToWorkdays(dtos);
  }

  async deleteAllWorkdays(): Promise<void> {
    await firstValueFrom(this.http.delete('/api/workdays'));
  }

  async getEntries(type?: 'standard' | 'correction'): Promise<Entry[]> {
    const params = type ? `?type=${type}` : '';
    const dtos = await firstValueFrom(
      this.http.get<EntryDto[]>(`/api/entries${params}`),
    );
    return dtos.map((dto) => this.convertEntryDtoToEntry(dto));
  }

  async updateEntries(request: WorklogUpdateRequest): Promise<Workday> {
    const requestDto: EntryUpdateRequest = {
      date: format(request.date, 'yyyy-MM-dd'),
      entries: request.entries.map((entry) => ({
        id: entry.id ?? null,
        type: entry.type,
        start: entry.start
          ? format(entry.start, "yyyy-MM-dd'T'HH:mm:ss")
          : null,
        end: entry.end ? format(entry.end, "yyyy-MM-dd'T'HH:mm:ss") : null,
        date: entry.date ? format(entry.date, 'yyyy-MM-dd') : null,
        duration: entry.duration ?? null,
      })),
    };
    const dto = await firstValueFrom(
      this.http.put<DayDto>('/api/entries', requestDto),
    );
    return this.convertDayDtoToWorkday(dto);
  }

  async deleteEntry(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`/api/entries/${id}`));
  }

  async getWorkStats(): Promise<Stat> {
    return firstValueFrom(this.http.get<Stat>('/api/worklog/stats'));
  }

  private convertDayDtosToWorkdays(dtos: DayDto[]): Workday[] {
    return dtos.map((dto) => this.convertDayDtoToWorkday(dto));
  }

  private convertDayDtoToWorkday(dto: DayDto): Workday {
    return {
      date: dto.date ? parseISO(dto.date) : new Date(),
      entries: dto.entries?.map((e) => this.convertEntryDtoToEntry(e)) ?? [],
      gross: dto.gross ?? '',
      balance: dto.balance ?? '',
      workday: dto.workday,
      user: dto.user ?? undefined,
    };
  }

  private convertEntryDtoToEntry(dto: EntryDto): Entry {
    return {
      id: dto.id ?? undefined,
      type: dto.type,
      start: dto.start ? parseISO(dto.start) : undefined,
      end: dto.end ? parseISO(dto.end) : undefined,
      date: dto.date ? parseISO(dto.date) : undefined,
      duration: dto.duration ?? undefined,
    };
  }
}
