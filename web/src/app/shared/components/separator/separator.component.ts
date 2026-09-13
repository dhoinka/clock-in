import {
  Component,
  input,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { cn } from '@/shared/utils/cn';

@Component({
  selector: 'zard-separator',
  standalone: true,
  host: {
    '[class]': 'classes()',
    role: 'separator',
  },
  changeDetection: ChangeDetectionStrategy.Eager,
  template: ``,
})
export class SeparatorComponent {
  class = input<string>('');
  orientation = input<'horizontal' | 'vertical'>('horizontal');

  classes = computed(() =>
    cn(
      'shrink-0 bg-border',
      this.orientation() === 'horizontal' ? 'h-[1px] w-full' : 'h-full w-[1px]',
      this.class(),
    ),
  );
}
