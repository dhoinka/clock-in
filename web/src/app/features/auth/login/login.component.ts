import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '@/core/services/auth.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardCardComponent } from '@/shared/components/card';
import {
  ZardFormControlComponent,
  ZardFormFieldComponent,
  ZardFormLabelComponent,
} from '@/shared/components/form';
import { ZardInputDirective } from '@/shared/components/input';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    ZardButtonComponent,
    ZardCardComponent,
    ZardFormLabelComponent,
    ZardFormFieldComponent,
    ZardFormControlComponent,
    ZardInputDirective,
    FormRoot,
    FormField,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private authService = inject(AuthService);

  loginModel = signal({
    username: '',
    password: '',
  });

  loginForm = form(
    this.loginModel,
    (schemaPath) => {
      required(schemaPath.username);
      required(schemaPath.password);
    },
    {
      submission: {
        action: async (field) => {
          this.usernameError.set('');
          this.passwordError.set('');
          this.loginError.set('');

          if (!field().value().username) {
            this.usernameError.set('Username is required');
            return;
          }
          if (!field().value().password) {
            this.passwordError.set('Password is required');
            return;
          }

          this.isSubmitting.set(true);
          try {
            await this.authService.login({
              username: field().value().username,
              password: field().value().password,
            });
          } catch {
            this.loginError.set('Wrong username / email address or password');
          } finally {
            this.isSubmitting.set(false);
          }
        },
      },
    },
  );

  loginError = signal<string>('');
  isSubmitting = signal<boolean>(false);
  usernameError = signal<string>('');
  passwordError = signal<string>('');
}
