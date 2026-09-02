import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgIcon } from '@ng-icons/core';
import { format, sub } from 'date-fns';
import { WorklogService } from '@/core/services/worklog.service';
import { Workday } from '@/core/models/worklog.model';
import { ZardSelectImports } from '@/shared/components/select';
import { ZardTableImports } from '@/shared/components/table';
import { ZardDialogService } from '@/shared/components/dialog';
import {
  BookingEditDialogComponent,
  BookingEditDialogData,
} from './booking-edit-dialog/booking-edit-dialog.component';
import { ZardSkeletonComponent } from '@/shared/components/skeleton';
import { ZardButtonComponent } from '@/shared/components/button';

interface MonthSelection {
  viewValue: string;
  value: string;
}

@Component({
  selector: 'app-bookings',
  imports: [
    CommonModule,
    ZardButtonComponent,
    ZardSkeletonComponent,
    ...ZardTableImports,
    NgIcon,
    ...ZardSelectImports,
  ],
  templateUrl: './bookings.component.html',
})
export class BookingsComponent implements OnInit {
  private worklogService = inject(WorklogService);
  private dialogService = inject(ZardDialogService);

  readonly skeletonRows = Array.from({ length: 30 }, (_, i) => i);
  readonly format = format;

  months = signal<MonthSelection[]>([]);
  selectedMonth = '';
  bookingsMonth = signal<Workday[]>([]);
  loading = signal(true);

  readonly selectedMonthLabel = computed(
    () =>
      this.months().find((m) => m.value === this.selectedMonth)?.viewValue ??
      '',
  );

  ngOnInit(): void {
    const monthOptions: MonthSelection[] = [];
    for (let i = 0; i < 5; i++) {
      const d = sub(new Date(), { months: i });
      monthOptions.push({
        value: format(d, 'yyyy-MM'),
        viewValue: format(d, 'MMMM yyyy'),
      });
    }
    this.months.set(monthOptions);
    this.selectedMonth = monthOptions[0].value;
    this.loadBookings(this.selectedMonth);
  }

  async loadBookings(month: string): Promise<void> {
    this.loading.set(true);
    try {
      const data = await this.worklogService.getWorkdaysByMonth(month);
      this.bookingsMonth.set(data);
    } catch (error) {
      console.error('Failed to load bookings:', error);
    } finally {
      this.loading.set(false);
    }
  }

  getRowClass(element: Workday): string {
    const today = new Date();
    const isToday =
      format(element.date, 'yyyy-MM-dd') === format(today, 'yyyy-MM-dd');
    if (isToday) return 'bg-accent/100';
    if (!element.workday) return 'bg-muted/30';
    return '';
  }

  getStartTime(element: Workday): string {
    const entry = element.entries.filter((e) => e.type !== 'correction')[0];
    return entry?.start ? format(entry.start, 'HH:mm') : '';
  }

  getEndTime(element: Workday): string {
    const filtered = element.entries.filter((e) => e.type !== 'correction');
    const entry = filtered[filtered.length - 1];
    return entry?.end ? format(entry.end, 'HH:mm') : '';
  }

  openEditDialog(row: Workday): void {
    const data: BookingEditDialogData = {
      row,
      onSaved: (updated: Workday) => {
        this.bookingsMonth.update((prev) =>
          prev.map((item) =>
            format(item.date, 'yyyy-MM-dd') ===
            format(updated.date, 'yyyy-MM-dd')
              ? updated
              : item,
          ),
        );
      },
    };
    this.dialogService.create({
      zTitle: 'Edit Time Entries',
      zContent: BookingEditDialogComponent,
      zData: data,
      zHideFooter: true,
      zWidth: '720px',
    });
  }
}
