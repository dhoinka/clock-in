import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { format, isSameDay, subDays } from 'date-fns';

import { Event } from '@/core/models/worklog.model';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogRef, Z_MODAL_DATA } from '@/shared/components/dialog';
import { ZardFormLabelComponent } from '@/shared/components/form';

export interface CalendarEventDialogData {
  event: Event;
  initialForm: { title: string; type: Event['type'] };
  onSave: (title: string, type: Event['type']) => Promise<void>;
  onDelete: () => Promise<void>;
}

@Component({
  selector: 'app-calendar-event-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ZardButtonComponent, ZardFormLabelComponent],
  templateUrl: './calendar-event-dialog.component.html',
})
export class CalendarEventDialogComponent {
  private dialogRef = inject(ZardDialogRef);
  readonly data = inject<CalendarEventDialogData>(Z_MODAL_DATA);

  readonly title = signal(this.data.initialForm.title);
  readonly type = signal<Event['type']>(this.data.initialForm.type);

  get formattedDate(): string {
    const start = this.data.event.start;
    const end = this.data.event.end;

    if (!start) {
      return '';
    }

    if (!end) {
      return format(start, 'PPP');
    }

    const displayEnd = this.data.event.allDay ? subDays(end, 1) : end;

    if (displayEnd < start || isSameDay(start, displayEnd)) {
      return format(start, 'PPP');
    }

    return `${format(start, 'PPP')} - ${format(displayEnd, 'PPP')}`;
  }

  close(): void {
    this.dialogRef.close();
  }

  async save(): Promise<void> {
    await this.data.onSave(this.title(), this.type());
    this.dialogRef.close();
  }

  async delete(): Promise<void> {
    await this.data.onDelete();
    this.dialogRef.close();
  }
}
