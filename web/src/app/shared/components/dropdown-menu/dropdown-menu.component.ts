import {
  Component,
  input,
  output,
  computed,
  signal,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { cn } from '@/shared/utils/cn';

@Component({
  selector: 'zard-dropdown-menu',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="relative inline-block text-left"
      (click)="$event.stopPropagation()"
    >
      <ng-content />
    </div>
  `,
})
export class DropdownMenuComponent {
  isOpen = signal(false);

  toggle() {
    this.isOpen.update((v) => !v);
  }

  close() {
    this.isOpen.set(false);
  }

  @HostListener('document:click')
  onDocumentClick() {
    this.isOpen.set(false);
  }
}

@Component({
  selector: 'zard-dropdown-menu-trigger',
  standalone: true,
  host: { '(click)': 'clickEvent.emit()' },
  template: `<ng-content />`,
})
export class DropdownMenuTriggerComponent {
  clickEvent = output<void>();
}

@Component({
  selector: 'zard-dropdown-menu-content',
  standalone: true,
  imports: [CommonModule],
  host: { '[class]': 'classes()' },
  template: `<ng-content />`,
})
export class DropdownMenuContentComponent {
  class = input<string>('');
  align = input<'start' | 'end' | 'center'>('end');
  side = input<'top' | 'bottom'>('bottom');

  classes = computed(() =>
    cn(
      'absolute z-50 min-w-[8rem] overflow-hidden rounded-md border bg-popover p-1 text-popover-foreground shadow-md',
      this.align() === 'end'
        ? 'right-0'
        : this.align() === 'start'
          ? 'left-0'
          : 'left-1/2 -translate-x-1/2',
      this.side() === 'top' ? 'bottom-full mb-1' : 'top-full mt-1',
      this.class(),
    ),
  );
}

@Component({
  selector: 'zard-dropdown-menu-item',
  standalone: true,
  host: {
    '[class]': 'classes()',
    '(click)': 'clickEvent.emit()',
  },
  template: `<ng-content />`,
})
export class DropdownMenuItemComponent {
  class = input<string>('');
  clickEvent = output<void>();

  classes = computed(() =>
    cn(
      'relative flex cursor-pointer select-none items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none transition-colors hover:bg-accent hover:text-accent-foreground',
      this.class(),
    ),
  );
}

@Component({
  selector: 'zard-dropdown-menu-separator',
  standalone: true,
  host: { class: '-mx-1 my-1 h-px bg-muted' },
  template: ``,
})
export class DropdownMenuSeparatorComponent {}
