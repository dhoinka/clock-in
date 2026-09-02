import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon } from '@ng-icons/core';
import { format } from 'date-fns';

import { Entry, Workday } from '@/core/models/worklog.model';
import { WorklogService } from '@/core/services/worklog.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogRef, Z_MODAL_DATA } from '@/shared/components/dialog';
import { ZardFormLabelComponent } from '@/shared/components/form';

export interface BookingEditDialogData {
  row: Workday;
  onSaved: (updated: Workday) => void;
}

interface EntryForm {
  type: 'standard' | 'correction';
  start: string;
  end: string;
  duration: string;
}

@Component({
  selector: 'app-booking-edit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ZardButtonComponent, ZardFormLabelComponent, NgIcon],
  templateUrl: './booking-edit-dialog.component.html',
})
export class BookingEditDialogComponent {
  private dialogRef = inject(ZardDialogRef);
  private worklogService = inject(WorklogService);
  readonly data = inject<BookingEditDialogData>(Z_MODAL_DATA);

  readonly format = format;

  readonly entryForms = signal<EntryForm[]>(
    this.data.row.entries.length > 0
      ? this.data.row.entries.map((e) => ({
          type: e.type,
          start: e.start ? format(e.start, 'HH:mm') : '',
          end: e.end ? format(e.end, 'HH:mm') : '',
          duration: e.duration ?? '',
        }))
      : [{ type: 'standard', start: '', end: '', duration: '' }],
  );

  readonly overlapError = signal('');

  readonly hasCorrectionEntry = computed(() =>
    this.entryForms().some((e) => e.type === 'correction'),
  );

  addEntry(type: 'standard' | 'correction'): void {
    this.entryForms.update((prev) => [
      ...prev,
      { type, start: '', end: '', duration: '' },
    ]);
  }

  removeEntry(index: number): void {
    this.entryForms.update((prev) => prev.filter((_, i) => i !== index));
  }

  closeDialog(): void {
    this.dialogRef.close();
  }

  async saveEntries(): Promise<void> {
    this.overlapError.set('');
    const row = this.data.row;
    const forms = this.entryForms();

    if (this.hasOverlappingEntries(forms)) {
      this.overlapError.set(
        'Some entries are overlapping. Please adjust the times.',
      );
      return;
    }

    const entries: Entry[] = forms.map((form, i) => {
      const original = row.entries[i];
      const dateStr = format(row.date, 'yyyy-MM-dd');
      return {
        id: original?.id,
        type: form.type,
        start: form.start ? new Date(`${dateStr}T${form.start}:00`) : undefined,
        end: form.end ? new Date(`${dateStr}T${form.end}:00`) : undefined,
        date: row.date,
        duration: form.duration || undefined,
      };
    });

    try {
      const result = await this.worklogService.updateEntries({
        date: row.date,
        entries,
      });
      this.data.onSaved(result);
      this.dialogRef.close();
    } catch (error) {
      console.error('Failed to update booking:', error);
    }
  }

  private hasOverlappingEntries(forms: EntryForm[]): boolean {
    const standard = forms.filter(
      (f) => f.type === 'standard' && f.start && f.end,
    );
    for (let i = 0; i < standard.length; i++) {
      for (let j = i + 1; j < standard.length; j++) {
        if (
          standard[i].start < standard[j].end &&
          standard[j].start < standard[i].end
        ) {
          return true;
        }
      }
    }
    return false;
  }
}
