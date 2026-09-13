import { Component, input, computed } from '@angular/core';
import { cn } from '@/shared/utils/cn';

@Component({
  selector: 'zard-separator',
  standalone: true,
  host: {
    '[class]': 'classes()',
    role: 'separator',
  },
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
