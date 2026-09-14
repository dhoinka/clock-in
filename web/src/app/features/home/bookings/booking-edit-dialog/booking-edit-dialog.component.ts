import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  applyEach,
  form,
  FormField,
  required,
  submit,
  validate,
} from '@angular/forms/signals';
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
  key: number;
  id?: number;
  type: 'standard' | 'correction';
  start: string;
  end: string;
  duration: string;
}

const DURATION_PATTERN =
  /^-?(?=.*\d+(?:ms|[dhms]))(?:\d+d)?\s*(?:\d+h)?\s*(?:\d+m)?\s*(?:\d+s)?\s*(?:\d+ms)?$/;

@Component({
  selector: 'app-booking-edit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormField, ZardButtonComponent, ZardFormLabelComponent, NgIcon],
  templateUrl: './booking-edit-dialog.component.html',
})
export class BookingEditDialogComponent {
  private dialogRef = inject(ZardDialogRef);
  private worklogService = inject(WorklogService);
  readonly data = inject<BookingEditDialogData>(Z_MODAL_DATA);

  readonly format = format;
  private nextEntryKey = 0;

  readonly entryModel = signal<EntryForm[]>(
    this.sortEntryForms(
      this.data.row.entries.length > 0
        ? this.data.row.entries.map((entry) => this.toEntryForm(entry))
        : [this.createEntryForm('standard')],
    ),
  );

  readonly entryForm = form(this.entryModel, (entries) => {
    applyEach(entries, (entry) => {
      required(entry.start, {
        message: 'Start time is required for standard entries.',
        when: ({ valueOf }) => valueOf(entry.type) === 'standard',
      });
      required(entry.duration, {
        message: 'Duration is required for a correction.',
        when: ({ valueOf }) => valueOf(entry.type) === 'correction',
      });
      validate(entry.end, ({ value, valueOf }) => {
        const start = valueOf(entry.start);
        return value() && start && value() < start
          ? {
              kind: 'endBeforeStart',
              message: 'End time must not be before the start time.',
            }
          : undefined;
      });
      validate(entry.duration, ({ value, valueOf }) =>
        valueOf(entry.type) === 'correction' &&
        value() &&
        !DURATION_PATTERN.test(value().trim())
          ? {
              kind: 'invalidDuration',
              message: 'Use a duration such as 30m, 1h, or -15m.',
            }
          : undefined,
      );
    });

    validate(entries, ({ value }) =>
      value().filter((entry) => entry.type === 'correction').length > 1
        ? {
            kind: 'multipleCorrections',
            message: 'Only one correction can be added per day.',
          }
        : undefined,
    );
    validate(entries, ({ value }) =>
      this.hasOverlappingEntries(value())
        ? {
            kind: 'overlappingEntries',
            message: 'Some entries overlap. Please adjust the times.',
          }
        : undefined,
    );
  });

  readonly saveError = signal('');
  readonly displayedError = computed(
    () =>
      this.saveError() ||
      (this.entryForm().touched()
        ? (this.entryForm().errorSummary()[0]?.message ?? '')
        : ''),
  );

  readonly hasCorrectionEntry = computed(() =>
    this.entryModel().some((entry) => entry.type === 'correction'),
  );

  addEntry(type: 'standard' | 'correction'): void {
    if (type === 'correction' && this.hasCorrectionEntry()) {
      return;
    }

    this.entryModel.update((prev) =>
      this.sortEntryForms([...prev, this.createEntryForm(type)]),
    );
  }

  sortEntries(): void {
    this.entryModel.update((entries) => this.sortEntryForms(entries));
  }

  removeEntry(key: number): void {
    this.entryModel.update((entries) =>
      entries.filter((entry) => entry.key !== key),
    );
  }

  closeDialog(): void {
    this.dialogRef.close();
  }

  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    this.saveError.set('');
    let savedWorkday: Workday | undefined;

    try {
      const succeeded = await submit(this.entryForm, {
        action: async (form) => {
          savedWorkday = await this.saveEntries(form().value());
          return undefined;
        },
        onInvalid: (form) => {
          form().errorSummary()[0]?.fieldTree().focusBoundControl();
        },
      });

      if (!succeeded || !savedWorkday) {
        return;
      }
    } catch (error) {
      console.error('Failed to update booking:', error);
      this.saveError.set('Could not save the changes. Please try again.');
      return;
    }

    try {
      this.data.onSaved(savedWorkday);
    } finally {
      this.dialogRef.close();
    }
  }

  private saveEntries(forms: EntryForm[]): Promise<Workday> {
    const row = this.data.row;
    const dateStr = format(row.date, 'yyyy-MM-dd');
    const entries: Entry[] = forms.map((entryForm) => ({
      id: entryForm.id,
      type: entryForm.type,
      start: entryForm.start
        ? new Date(`${dateStr}T${entryForm.start}:00`)
        : undefined,
      end: entryForm.end
        ? new Date(`${dateStr}T${entryForm.end}:00`)
        : undefined,
      date: row.date,
      duration: entryForm.duration || undefined,
    }));

    return this.worklogService.updateEntries({ date: row.date, entries });
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

  private sortEntryForms(entries: EntryForm[]): EntryForm[] {
    return [...entries].sort((a, b) => {
      if (a.type === b.type) return 0;
      return a.type === 'correction' ? 1 : -1;
    });
  }

  private createEntryForm(type: EntryForm['type']): EntryForm {
    return {
      key: this.nextEntryKey++,
      type,
      start: '',
      end: '',
      duration: '',
    };
  }

  private toEntryForm(entry: Entry): EntryForm {
    return {
      key: this.nextEntryKey++,
      id: entry.id,
      type: entry.type,
      start: entry.start ? format(entry.start, 'HH:mm') : '',
      end: entry.end ? format(entry.end, 'HH:mm') : '',
      duration: entry.duration ?? '',
    };
  }
}
