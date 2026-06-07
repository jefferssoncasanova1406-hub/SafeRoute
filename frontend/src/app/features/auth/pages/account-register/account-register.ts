import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { RegisterRequest, RegisterResponse } from '../../models/register.model';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

const passwordsMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-account-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './account-register.html',
  styleUrl: './account-register.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountRegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  protected readonly isSubmitting = signal(false);
  protected readonly registerSuccess = signal<RegisterResponse | null>(null);
  protected readonly registerError = signal<string | null>(null);

  protected readonly registerForm = this.fb.nonNullable.group(
    {
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(255)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator },
  );

  protected submit(): void {
    this.registerError.set(null);
    this.registerSuccess.set(null);

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const payload: RegisterRequest = {
      nombre: this.registerForm.controls.nombre.value.trim(),
      email: this.registerForm.controls.email.value.trim(),
      password: this.registerForm.controls.password.value,
    };

    this.isSubmitting.set(true);

    this.authService
      .register(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.registerSuccess.set(response);
          this.registerForm.reset();
        },
        error: (error: HttpErrorResponse) => {
          this.registerError.set(this.parseError(error));
        },
      });
  }

  protected hasError(
    controlName: 'nombre' | 'email' | 'password' | 'confirmPassword',
  ): boolean {
    const control = this.registerForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  protected hasPasswordMismatch(): boolean {
    return Boolean(
      this.registerForm.errors?.['passwordMismatch'] &&
        (this.registerForm.controls.confirmPassword.touched ||
          this.registerForm.controls.confirmPassword.dirty),
    );
  }

  private parseError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'No pudimos conectar con el servicio en este momento. Intenta nuevamente.';
    }

    const body = error.error as ApiErrorBody | string | null;

    if (typeof body === 'string' && body.trim()) {
      return body;
    }

    if (body && typeof body === 'object') {
      const details = body.details ? Object.values(body.details).join(' ') : '';

      if (details) {
        return details;
      }

      if (body.message) {
        return body.message;
      }
    }

    return 'No fue posible crear la cuenta en este momento.';
  }
}
