import { Component, inject, effect } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@/core/services/auth.service';

@Component({
  selector: 'app-root-redirect',
  standalone: true,
  templateUrl: './root.component.html',
})
export class RootComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  constructor() {
    effect(() => {
      if (!this.authService.isLoading()) {
        if (this.authService.isAuthenticated()) {
          this.router.navigate(['/home']);
        } else {
          this.router.navigate(['/login']);
        }
      }
    });
  }
}
