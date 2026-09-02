import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDialogRef, Z_MODAL_DATA } from '@/shared/components/dialog';

export interface DeleteConfirmDialogData {
  onConfirm: () => Promise<void>;
}

@Component({
  selector: 'app-delete-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ZardButtonComponent],
  template: `
    <p class="text-muted-foreground text-sm">This action can't be undone</p>

    <div
      class="mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end sm:gap-0 sm:space-x-2"
    >
      <button z-button zType="outline" (click)="close()">Cancel</button>
      <button z-button zType="destructive" (click)="confirm()">Delete</button>
    </div>
  `,
})
export class DeleteConfirmDialogComponent {
  private dialogRef = inject(ZardDialogRef);
  readonly data = inject<DeleteConfirmDialogData>(Z_MODAL_DATA);

  close(): void {
    this.dialogRef.close();
  }

  async confirm(): Promise<void> {
    await this.data.onConfirm();
    this.dialogRef.close();
  }
}
