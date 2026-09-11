import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon } from '@ng-icons/core';
import { format } from 'date-fns';
import { WorklogService } from '@/core/services/worklog.service';
import { Entry, StatViewModel } from '@/core/models/worklog.model';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardDialogService } from '@/shared/components/dialog';
import { ZardButtonComponent } from '@/shared/components/button';
import {
  DeleteConfirmDialogComponent,
  DeleteConfirmDialogData,
} from './delete-confirm-dialog/delete-confirm-dialog.component';

@Component({
  selector: 'app-overview',
  imports: [CommonModule, ZardButtonComponent, ZardCardComponent, NgIcon],
  templateUrl: './overview.component.html',
})
export class OverviewComponent implements OnInit {
  private worklogService = inject(WorklogService);
  private dialogService = inject(ZardDialogService);

  readonly format = format;

  activeTab = signal('corrections');
  entries = signal<Entry[]>([]);
  stats = signal<StatViewModel | null>(null);
  isLoading = signal(true);
  entryToDelete = signal<Entry | null>(null);

  tabClass(tab: string): string {
    const base =
      'inline-flex items-center justify-center whitespace-nowrap rounded-sm px-3 py-1.5 text-sm font-medium transition-all cursor-pointer';
    return this.activeTab() === tab
      ? `${base} bg-background text-foreground shadow-sm`
      : `${base} hover:bg-background/50`;
  }

  ngOnInit(): void {
    this.loadData();
  }

  private async loadData(): Promise<void> {
    this.isLoading.set(true);
    try {
      await Promise.all([this.loadCorrections(), this.loadStats()]);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      this.isLoading.set(false);
    }
  }

  private async loadCorrections(): Promise<void> {
    const corrections = await this.worklogService.getEntries('correction');
    this.entries.set(corrections);
  }

  private async loadStats(): Promise<void> {
    const statsData = await this.worklogService.getWorkStats();
    if (statsData.avgStart !== null && statsData.avgEnd !== null) {
      this.stats.set({
        avgStart: this.formatTime(statsData.avgStart),
        avgEnd: this.formatTime(statsData.avgEnd),
      });
    }
  }

  private formatTime(time: number): string {
    const hours = Math.floor(time);
    const minutes = Math.round((time - hours) * 60);
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
  }

  openDeleteConfirm(entry: Entry): void {
    this.entryToDelete.set(entry);
    const data: DeleteConfirmDialogData = {
      onConfirm: () => this.confirmDelete(),
    };
    this.dialogService.create({
      zTitle: 'Are you sure?',
      zContent: DeleteConfirmDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '420px',
    });
  }

  async confirmDelete(): Promise<void> {
    const entry = this.entryToDelete();
    if (!entry?.id) return;
    try {
      await this.worklogService.deleteEntry(entry.id);
      await this.loadCorrections();
    } catch (error) {
      console.error('Error deleting entry:', error);
    } finally {
      this.entryToDelete.set(null);
    }
  }
}
