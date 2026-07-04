import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { RiskPreference, UpdateProfileRequest } from '../../models/profile.model';
import { ProfileService } from '../../services/profile.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

const passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('newPassword')?.value;
  const confirmation = control.get('confirmPassword')?.value;
  return password && confirmation && password !== confirmation ? { passwordMismatch: true } : null;
};

@Component({
  selector: 'app-profile-settings-page',
  imports: [ReactiveFormsModule],
  templateUrl: './profile-settings.html',
  styleUrl: './profile-settings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileSettingsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly profileService = inject(ProfileService);

  protected readonly isLoading = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly isChangingPassword = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly profileError = signal<string | null>(null);
  protected readonly profileSuccess = signal<string | null>(null);
  protected readonly passwordError = signal<string | null>(null);
  protected readonly passwordSuccess = signal<string | null>(null);

  protected readonly profileForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: [{ value: '', disabled: true }],
    preferenciasRiesg: ['medio', Validators.required],
    radioAlerta: [1, [Validators.required, Validators.min(0.1), Validators.max(20)]],
    notificacionesActi: [true],
  });

  protected readonly passwordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required, Validators.maxLength(255)]],
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(255)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  ngOnInit(): void {
    this.loadProfile();
  }

  protected loadProfile(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.profileService
      .getProfile()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (profile) =>
          this.profileForm.reset({
            nombre: profile.nombre,
            email: profile.email,
            preferenciasRiesg: profile.preferenciasRiesg || 'medio',
            radioAlerta: Number(profile.radioAlerta),
            notificacionesActi: profile.notificacionesActi,
          }),
        error: (error: HttpErrorResponse) =>
          this.loadError.set(this.parseError(error, 'No se pudo cargar tu perfil.')),
      });
  }

  protected saveProfile(): void {
    this.profileError.set(null);
    this.profileSuccess.set(null);
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const value = this.profileForm.getRawValue();
    const payload: UpdateProfileRequest = {
      nombre: value.nombre.trim(),
      preferenciasRiesg: value.preferenciasRiesg as RiskPreference,
      radioAlerta: Number(value.radioAlerta),
      notificacionesActi: value.notificacionesActi,
    };

    this.isSaving.set(true);
    this.profileService
      .updateProfile(payload)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (profile) => {
          this.auth.updateSessionName(profile.nombre);
          this.profileForm.markAsPristine();
          this.profileSuccess.set('Tu perfil y preferencias se guardaron correctamente.');
        },
        error: (error: HttpErrorResponse) =>
          this.profileError.set(this.parseError(error, 'No se pudieron guardar los cambios.')),
      });
  }

  protected changePassword(): void {
    this.passwordError.set(null);
    this.passwordSuccess.set(null);
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.isChangingPassword.set(true);
    this.profileService
      .changePassword({
        currentPassword: this.passwordForm.controls.currentPassword.value,
        newPassword: this.passwordForm.controls.newPassword.value,
      })
      .pipe(finalize(() => this.isChangingPassword.set(false)))
      .subscribe({
        next: (message) => {
          this.passwordForm.reset();
          this.passwordSuccess.set(message);
        },
        error: (error: HttpErrorResponse) =>
          this.passwordError.set(this.parseError(error, 'No se pudo actualizar la contraseña.')),
      });
  }

  private parseError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) return 'No se pudo conectar con el servidor.';
    const body = error.error as ApiErrorBody | string | null;
    if (typeof body === 'string' && body.trim()) return body;
    if (body && typeof body === 'object') {
      const details = body.details ? Object.values(body.details).join(' ') : '';
      return details || body.message || fallback;
    }
    return fallback;
  }
}
