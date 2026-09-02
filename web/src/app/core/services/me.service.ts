import { inject, Injectable } from '@angular/core';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class MeService {
  private authService = inject(AuthService);

  getName(): string {
    const user = this.authService.user();
    if (!user) return '';
    return user.name || user.username || '';
  }
}
