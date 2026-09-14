import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideIcons } from '@ng-icons/core';
import {
  lucidePlus,
  lucideTrash2,
  lucideTriangleAlert,
} from '@ng-icons/lucide';
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

  let fixture: ComponentFixture<BookingEditDialogComponent>;
  let component: BookingEditDialogComponent;

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [BookingEditDialogComponent],
      providers: [
        provideIcons({ lucidePlus, lucideTrash2, lucideTriangleAlert }),
        { provide: WorklogService, useValue: { updateEntries } },
        { provide: ZardDialogRef, useValue: { close } },
        { provide: Z_MODAL_DATA, useValue: { row, onSaved } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(BookingEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
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

  it('updates and reorders entries through the rendered signal form', async () => {
    const correction = component
      .entryModel()
      .find((entry) => entry.type === 'correction');
    component.removeEntry(correction!.key);
    component.addEntry('standard');
    fixture.detectChanges();

    const firstTypeSelect = fixture.nativeElement.querySelector(
      'select',
    ) as HTMLSelectElement;
    firstTypeSelect.value = 'correction';
    firstTypeSelect.dispatchEvent(new Event('input', { bubbles: true }));
    firstTypeSelect.dispatchEvent(new Event('change', { bubbles: true }));
    await fixture.whenStable();

    expect(component.entryModel().map((entry) => entry.type)).toEqual([
      'standard',
      'correction',
    ]);
  });

  it('submits the rendered form with entries keeping their original ids', async () => {
    updateEntries.mockResolvedValue(row);

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    await fixture.whenStable();

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

  it('keeps the controls disabled while a save is in progress', async () => {
    let resolveUpdate: (value: Workday) => void = () => undefined;
    updateEntries.mockReturnValue(
      new Promise<Workday>((resolve) => {
        resolveUpdate = resolve;
      }),
    );

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
    await Promise.resolve();
    fixture.detectChanges();

    const fieldset = fixture.nativeElement.querySelector(
      'fieldset',
    ) as HTMLFieldSetElement;
    expect(fieldset.disabled).toBe(true);

    resolveUpdate(row);
    await fixture.whenStable();
  });
});
