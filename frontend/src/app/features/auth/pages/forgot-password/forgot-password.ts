import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  protected readonly isSubmitting = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
  });

  protected submit(): void {
    this.requestError.set(null);
    this.successMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.auth
      .requestPasswordReset(this.form.controls.email.value.trim())
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (message) => this.successMessage.set(message),
        error: (error: HttpErrorResponse) =>
          this.requestError.set(
            error.status === 0
              ? 'No se pudo conectar con el servicio.'
              : 'No fue posible procesar la solicitud.',
          ),
      });
  }
}
