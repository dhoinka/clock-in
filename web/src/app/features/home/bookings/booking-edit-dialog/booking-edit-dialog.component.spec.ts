import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Workday } from '@/core/models/worklog.model';
import { WorklogService } from '@/core/services/worklog.service';
import { ZardDialogRef, Z_MODAL_DATA } from '@/shared/components/dialog';
import { BookingEditDialogComponent } from './booking-edit-dialog.component';

describe('BookingEditDialogComponent', () => {
  const row: Workday = {
    date: new Date(2026, 8, 8),
    entries: [
      { id: 2, type: 'correction', duration: '30m' },
      {
        id: 1,
        type: 'standard',
        start: new Date(2026, 8, 8, 8),
        end: new Date(2026, 8, 8, 16),
      },
    ],
    gross: '08:00',
    balance: '00:30',
    workday: true,
  };
  const updateEntries = vi.fn();
  const onSaved = vi.fn();
  const close = vi.fn();

  let component: BookingEditDialogComponent;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: WorklogService, useValue: { updateEntries } },
        { provide: ZardDialogRef, useValue: { close } },
        { provide: Z_MODAL_DATA, useValue: { row, onSaved } },
      ],
    });
    component = TestBed.runInInjectionContext(
      () => new BookingEditDialogComponent(),
    );
  });

  it('sorts corrections last while preserving entry identity', () => {
    expect(
      component.entryModel().map(({ id, type }) => ({ id, type })),
    ).toEqual([
      { id: 1, type: 'standard' },
      { id: 2, type: 'correction' },
    ]);
  });

  it('uses the signal form as the source of truth', () => {
    component.entryForm[0].start().value.set('09:15');

    expect(component.entryModel()[0].start).toBe('09:15');
  });

  it('does not add a second correction', () => {
    component.addEntry('correction');

    expect(
      component.entryModel().filter((entry) => entry.type === 'correction'),
    ).toHaveLength(1);
  });

  it('submits sorted entries with their own original ids', async () => {
    updateEntries.mockResolvedValue(row);

    await component.onSubmit(new Event('submit'));

    expect(updateEntries).toHaveBeenCalledWith({
      date: row.date,
      entries: [
        expect.objectContaining({ id: 1, type: 'standard' }),
        expect.objectContaining({ id: 2, type: 'correction' }),
      ],
    });
    expect(onSaved).toHaveBeenCalledWith(row);
    expect(close).toHaveBeenCalledOnce();
  });
});
