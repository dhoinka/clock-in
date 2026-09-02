import { z } from 'zod';

export type EntryType = 'standard' | 'correction';
export type EventType = 'none' | 'vacation' | 'sick';
export type EventStatus = 'new' | '';

export const statusResponseSchema = z.object({
  checkedIn: z.boolean(),
  gross: z.string().nullable(),
  balance: z.string().nullable(),
});

export type StatusResponse = z.infer<typeof statusResponseSchema>;

export interface CheckInModel {
  checkedIn: boolean;
  balance: string;
  gross: string;
}

export interface Entry {
  id?: number;
  type: EntryType;
  start?: Date;
  end?: Date;
  date?: Date;
  duration?: string;
}

export interface EntryDto {
  id?: number | null;
  type: EntryType;
  start?: string | null;
  end?: string | null;
  duration?: string | null;
  date?: string | null;
}

export interface DayDto {
  date: string;
  entries: EntryDto[] | null;
  gross: string | null;
  balance: string | null;
  workday: boolean;
  user: string | null;
}

export interface Workday {
  date: Date;
  entries: Entry[];
  gross: string;
  balance: string;
  workday: boolean;
  user?: string;
}

export interface WorklogUpdateRequest {
  date: Date;
  entries: Entry[];
}

export interface EntryUpdateRequest {
  date: string | null;
  entries: EntryDto[];
}

export interface CalendarEvent {
  id: string;
  events: Event[];
  color: string;
}

export interface Event {
  id?: number;
  title: string;
  type: EventType;
  status: EventStatus;
  start: Date;
  end: Date;
  allDay: boolean;
  user?: string;
}

export const holidaySchema = z.object({
  date: z.string(),
  name: z.string(),
  allStates: z.boolean(),
});

export type Holiday = z.infer<typeof holidaySchema>;

export interface Stat {
  username: string | null;
  avgStart: number | null;
  avgEnd: number | null;
}

export interface StatViewModel {
  avgStart: string;
  avgEnd: string;
}

export interface WorklogSettings {
  workingHours: string;
  workingDays: boolean[];
  breakTime: string;
}

export interface WorklogSettingsDto {
  workingHours: string;
  workingDays: number;
  breakTime: string;
}
