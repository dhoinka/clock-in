import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { WorklogSettings, WorklogSettingsDto } from '../models/worklog.model';

@Injectable({ providedIn: 'root' })
export class WorklogSettingsService {
  private http = inject(HttpClient);

  async getSettings(): Promise<WorklogSettings> {
    const dto = await firstValueFrom(
      this.http.get<WorklogSettingsDto>('/api/settings'),
    );
    return this.convertDtoToSettings(dto);
  }

  async saveSettings(settings: WorklogSettings): Promise<void> {
    const dto = this.convertSettingsToDto(settings);
    await firstValueFrom(this.http.put('/api/settings', dto));
  }

  private convertDtoToSettings(dto: WorklogSettingsDto): WorklogSettings {
    const bitmask = dto.workingDays ?? 0;
    const workingDays = Array.from(
      { length: 7 },
      (_, i) => !!(bitmask & (1 << i)),
    );
    return {
      workingHours: dto.workingHours ?? '',
      workingDays,
      breakTime: dto.breakTime ?? '',
    };
  }

  private convertSettingsToDto(settings: WorklogSettings): WorklogSettingsDto {
    const bitmask = settings.workingDays.reduce(
      (acc, day, i) => acc | (day ? 1 << i : 0),
      0,
    );
    return {
      workingHours: settings.workingHours,
      workingDays: bitmask,
      breakTime: settings.breakTime,
    };
  }
}
