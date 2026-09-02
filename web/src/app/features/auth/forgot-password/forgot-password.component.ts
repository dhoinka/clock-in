import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ZardButtonComponent } from '../../../shared/components/button/button.component';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardFormLabelComponent } from '@/shared/components/form';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    ZardButtonComponent,
    ZardCardComponent,
    ZardFormLabelComponent,
  ],
  templateUrl: './forgot-password.component.html',
})
export class ForgotPasswordComponent {
  private authService = inject(AuthService);

  email = '';
  isSubmitting = signal(false);
  submitted = signal(false);
  error = signal('');

  async onSubmit(): Promise<void> {
    if (!this.email) return;
    this.isSubmitting.set(true);
    this.error.set('');
    try {
      await this.authService.forgotPassword({ email: this.email });
      this.submitted.set(true);
    } catch {
      this.error.set('Failed to send reset email. Please try again.');
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
