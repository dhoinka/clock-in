import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorklogService } from '@/core/services/worklog.service';
import { WorklogSettingsService } from '@/core/services/worklog-settings.service';
import { WorklogSettings } from '@/core/models/worklog.model';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardFormLabelComponent } from '@/shared/components/form';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogService } from '@/shared/components/dialog';
import {
  DeleteAllEntriesDialogComponent,
  DeleteAllEntriesDialogData,
} from './delete-all-entries-dialog/delete-all-entries-dialog.component';
import { ThemeService } from '@/core/services/theme.service';

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

@Component({
  selector: 'app-settings',
  imports: [
    CommonModule,
    FormsModule,
    ZardCardComponent,
    ZardFormLabelComponent,
    ZardButtonComponent,
  ],
  templateUrl: './settings.component.html',
})
export class SettingsComponent implements OnInit {
  private worklogService = inject(WorklogService);
  private worklogSettingsService = inject(WorklogSettingsService);
  private dialogService = inject(ZardDialogService);
  private themeService = inject(ThemeService);

  readonly days = DAYS;

  readonly tabs = [
    { value: 'booking', label: 'Booking' },
    { value: 'preferences', label: 'Preferences' },
  ];

  activeTab = signal('booking');
  readonly theme = this.themeService.theme;

  // Settings state
  settings: WorklogSettings = {
    workingHours: '',
    workingDays: new Array(7).fill(false),
    breakTime: '',
  };
  originalSettings: WorklogSettings | null = null;
  settingsLoading = signal(true);
  settingsSaving = signal(false);
  settingsError = signal('');

  readonly isSettingsDirty = computed(
    () =>
      JSON.stringify(this.settings) !== JSON.stringify(this.originalSettings),
  );

  ngOnInit(): void {
    this.loadSettings();
  }

  tabClass(tab: string): string {
    const base =
      'inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium transition-all cursor-pointer';
    return this.activeTab() === tab
      ? `${base} bg-background text-foreground shadow-sm`
      : `${base} hover:bg-background/50`;
  }

  dayBtnClass(index: number): string {
    const isActive = this.settings.workingDays[index];
    return isActive
      ? 'inline-flex items-center justify-center rounded-md bg-primary text-primary-foreground h-9 w-12 text-sm font-medium'
      : 'inline-flex items-center justify-center rounded-md border border-input bg-background h-9 w-12 text-sm font-medium hover:bg-accent';
  }

  toggleDay(index: number): void {
    this.settings = {
      ...this.settings,
      workingDays: this.settings.workingDays.map((d, i) =>
        i === index ? !d : d,
      ),
    };
  }

  private async loadSettings(): Promise<void> {
    this.settingsLoading.set(true);
    try {
      const s = await this.worklogSettingsService.getSettings();
      this.settings = { ...s };
      this.originalSettings = { ...s };
    } catch {
      this.settingsError.set('Failed to load booking settings');
    } finally {
      this.settingsLoading.set(false);
    }
  }

  async saveSettings(): Promise<void> {
    this.settingsSaving.set(true);
    this.settingsError.set('');
    try {
      await this.worklogSettingsService.saveSettings(this.settings);
      this.originalSettings = { ...this.settings };
    } catch {
      this.settingsError.set('Failed to save booking settings');
    } finally {
      this.settingsSaving.set(false);
    }
  }

  resetSettings(): void {
    if (this.originalSettings) {
      this.settings = { ...this.originalSettings };
    }
  }

  openDeleteAllConfirm(): void {
    const data: DeleteAllEntriesDialogData = {
      onConfirm: () => this.deleteAllEntries(),
    };
    this.dialogService.create({
      zTitle: 'Delete All Time Entries',
      zContent: DeleteAllEntriesDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '520px',
    });
  }

  private async deleteAllEntries(): Promise<void> {
    try {
      await this.worklogService.deleteAllWorkdays();
    } catch {
      this.settingsError.set('Failed to delete time entries');
    }
  }

  setTheme(t: 'light' | 'dark' | 'system'): void {
    this.themeService.setTheme(t);
  }
}
