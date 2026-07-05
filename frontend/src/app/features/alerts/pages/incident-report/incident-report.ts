import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { IncidentReportRequest } from '../../models/alert-community.model';
import { AlertCommunityService } from '../../services/alert-community.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

const trimmedRequired: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  String(control.value ?? '').trim() ? null : { trimmedRequired: true };

const trimmedLength = (min: number, max: number): ValidatorFn => (
  control: AbstractControl,
): ValidationErrors | null => {
  const length = String(control.value ?? '').trim().length;
  if (length < min) return { trimmedMinLength: { requiredLength: min, actualLength: length } };
  if (length > max) return { trimmedMaxLength: { requiredLength: max, actualLength: length } };
  return null;
};

@Component({
  selector: 'app-incident-report-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './incident-report.html',
  styleUrl: './incident-report.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncidentReportPage {
  private readonly fb = inject(FormBuilder);
  private readonly alertCommunityService = inject(AlertCommunityService);

  protected readonly isSubmitting = signal(false);
  protected readonly reportSuccess = signal<string | null>(null);
  protected readonly reportError = signal<string | null>(null);

  protected readonly reportForm = this.fb.nonNullable.group({
    tipoIncidente: ['', [trimmedRequired]],
    ubicacion: ['', [trimmedRequired]],
    descripcion: ['', [trimmedRequired, trimmedLength(10, 500)]],
  });

  protected submit(): void {
    this.reportSuccess.set(null);
    this.reportError.set(null);

    if (this.isSubmitting()) {
      return;
    }

    if (this.reportForm.invalid) {
      this.reportForm.markAllAsTouched();
      return;
    }

    const value = this.reportForm.getRawValue();
    const payload: IncidentReportRequest = {
      tipoIncidente: value.tipoIncidente.trim(),
      ubicacion: value.ubicacion.trim(),
      descripcion: value.descripcion.trim(),
    };

    this.isSubmitting.set(true);

    this.alertCommunityService
      .createReport(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.reportSuccess.set(response.message || 'Tu reporte fue registrado correctamente.');
          this.reportForm.reset();
        },
        error: (error: HttpErrorResponse) => {
          this.reportError.set(this.parseError(error, 'No fue posible registrar el reporte.'));
        },
      });
  }

  protected hasError(controlName: 'tipoIncidente' | 'ubicacion' | 'descripcion'): boolean {
    const control = this.reportForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  protected descriptionLength(): number {
    return this.reportForm.controls.descripcion.value.trim().length;
  }

  private parseError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) return 'No se pudo conectar con el servidor.';

    const body = error.error as ApiErrorBody | string | null;

    if (typeof body === 'string' && body.trim()) return body;

    if (body && typeof body === 'object') {
      const details = body.details ? Object.values(body.details).join(' ') : '';
      if (details) return details;
      if (body.message) return body.message;
    }

    if (error.status === 400) return 'Revisa los campos del reporte.';
    if (error.status === 401 || error.status === 403) return 'No tienes autorización para reportar incidentes.';

    return fallback;
  }
}
