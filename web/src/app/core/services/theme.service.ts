import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<'light' | 'dark' | 'system'>('system');

  setTheme(t: 'light' | 'dark' | 'system'): void {
    this.theme.set(t);
    this.applyTheme(t);
    localStorage.setItem('theme', t);
  }

  loadTheme(): void {
    const saved = localStorage.getItem('theme') as
      | 'light'
      | 'dark'
      | 'system'
      | null;
    if (saved) {
      this.theme.set(saved);
      this.applyTheme(saved);
    } else {
      this.applyTheme('system');
    }
  }

  private applyTheme(t: 'light' | 'dark' | 'system'): void {
    if (t === 'dark') {
      document.documentElement.classList.add('dark');
    } else if (t === 'light') {
      document.documentElement.classList.remove('dark');
    } else {
      const prefersDark = window.matchMedia(
        '(prefers-color-scheme: dark)',
      ).matches;
      document.documentElement.classList.toggle('dark', prefersDark);
    }
  }
}
