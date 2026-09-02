import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '@/core/services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceMock: { login: ReturnType<typeof vi.fn> };

  const submitForm = async (): Promise<void> => {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  };

  beforeEach(async () => {
    authServiceMock = { login: vi.fn().mockResolvedValue(undefined) };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialise signals to empty / false', () => {
    expect(component.loginError()).toBe('');
    expect(component.usernameError()).toBe('');
    expect(component.passwordError()).toBe('');
    expect(component.isSubmitting()).toBe(false);
  });

  it('should not submit when username is empty', async () => {
    component.loginModel.set({ username: '', password: '' });
    fixture.detectChanges();

    await submitForm();

    expect(component.usernameError()).toBe('');
    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('should not submit when password is empty', async () => {
    component.loginModel.set({ username: 'user@example.com', password: '' });
    fixture.detectChanges();

    await submitForm();

    expect(component.passwordError()).toBe('');
    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('should call authService.login with the form data on valid submit', async () => {
    component.loginModel.set({
      username: 'user@example.com',
      password: 'secret',
    });
    fixture.detectChanges();

    await submitForm();

    expect(authServiceMock.login).toHaveBeenCalledWith({
      username: 'user@example.com',
      password: 'secret',
    });
  });

  it('should clear previous errors before submitting', async () => {
    component.usernameError.set('old error');
    component.passwordError.set('old error');
    component.loginError.set('old error');
    component.loginModel.set({
      username: 'user@example.com',
      password: 'secret',
    });
    fixture.detectChanges();

    await submitForm();

    expect(component.usernameError()).toBe('');
    expect(component.passwordError()).toBe('');
    expect(component.loginError()).toBe('');
  });

  it('should set loginError when authService.login rejects', async () => {
    authServiceMock.login.mockRejectedValue(new Error('Unauthorized'));
    component.loginModel.set({
      username: 'user@example.com',
      password: 'wrong',
    });
    fixture.detectChanges();

    await submitForm();

    expect(component.loginError()).toBe(
      'Wrong username / email address or password',
    );
  });

  it('should reset isSubmitting to false after successful login', async () => {
    component.loginModel.set({
      username: 'user@example.com',
      password: 'secret',
    });
    fixture.detectChanges();

    await submitForm();

    expect(component.isSubmitting()).toBe(false);
  });

  it('should reset isSubmitting to false after failed login', async () => {
    authServiceMock.login.mockRejectedValue(new Error('Unauthorized'));
    component.loginModel.set({
      username: 'user@example.com',
      password: 'wrong',
    });
    fixture.detectChanges();

    await submitForm();

    expect(component.isSubmitting()).toBe(false);
  });
});
