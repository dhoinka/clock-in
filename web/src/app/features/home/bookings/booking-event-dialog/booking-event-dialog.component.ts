import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { format, parseISO } from 'date-fns';

import { Event } from '@/core/models/worklog.model';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogRef, Z_MODAL_DATA } from '@/shared/components/dialog';
import { ZardFormLabelComponent } from '@/shared/components/form';

export interface BookingEventDialogData {
  event?: Event;
  initialDate: Date;
  onSave: (event: Event) => Promise<void>;
  onDelete?: () => Promise<void>;
}

@Component({
  selector: 'app-booking-event-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ZardButtonComponent, ZardFormLabelComponent],
  templateUrl: './booking-event-dialog.component.html',
})
export class BookingEventDialogComponent {
  private readonly dialogRef = inject(ZardDialogRef);
  readonly data = inject<BookingEventDialogData>(Z_MODAL_DATA);

  readonly title = signal(this.data.event?.title ?? '');
  readonly type = signal<Event['type']>(this.data.event?.type ?? 'vacation');
  readonly startDate = signal(
    format(this.data.event?.start ?? this.data.initialDate, 'yyyy-MM-dd'),
  );
  readonly endDate = signal(
    format(this.data.event?.end ?? this.data.initialDate, 'yyyy-MM-dd'),
  );
  readonly error = signal('');
  readonly saving = signal(false);

  setType(value: string): void {
    if (value === 'vacation' || value === 'sick' || value === 'other') {
      this.type.set(value);
    }
  }

  close(): void {
    this.dialogRef.close();
  }

  async save(): Promise<void> {
    const title = this.title().trim();
    const start = this.startDate();
    const end = this.endDate();

    if (!title) {
      this.error.set('Enter an event title.');
      return;
    }
    if (!start || !end) {
      this.error.set('Choose both a start and end date.');
      return;
    }
    if (end < start) {
      this.error.set('The end date must be on or after the start date.');
      return;
    }

    this.error.set('');
    this.saving.set(true);
    try {
      await this.data.onSave({
        id: this.data.event?.id,
        title,
        type: this.type(),
        status: this.data.event?.status ?? 'new',
        start: parseISO(start),
        end: parseISO(end),
        allDay: true,
      });
      this.dialogRef.close();
    } catch {
      this.error.set('The event could not be saved. Please try again.');
    } finally {
      this.saving.set(false);
    }
  }

  async delete(): Promise<void> {
    if (!this.data.onDelete) {
      return;
    }

    this.error.set('');
    this.saving.set(true);
    try {
      await this.data.onDelete();
      this.dialogRef.close();
    } catch {
      this.error.set('The event could not be deleted. Please try again.');
    } finally {
      this.saving.set(false);
    }
  }
}
