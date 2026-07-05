import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import {
  AlertHistoryFilters,
  AlertHistoryItem,
} from '../../models/alert-community.model';
import { AlertCommunityService } from '../../services/alert-community.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

const dateRangeValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const fechaInicio = String(control.get('fechaInicio')?.value ?? '');
  const fechaFin = String(control.get('fechaFin')?.value ?? '');

  if (!fechaInicio || !fechaFin) {
    return null;
  }

  return fechaFin < fechaInicio ? { invalidDateRange: true } : null;
};

@Component({
  selector: 'app-alert-history-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './alert-history.html',
  styleUrl: './alert-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertHistoryPage {
  private readonly fb = inject(FormBuilder);
  private readonly alertCommunityService = inject(AlertCommunityService);

  protected readonly isLoading = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly history = signal<AlertHistoryItem[]>([]);

  protected readonly sortedHistory = computed(() =>
    [...this.history()].sort(
      (a, b) => this.dateTimeValue(b.fechaEmision) - this.dateTimeValue(a.fechaEmision),
    ),
  );

  protected readonly filterForm = this.fb.nonNullable.group(
    {
      tipoIncidente: [''],
      estado: [''],
      fechaInicio: [''],
      fechaFin: [''],
    },
    { validators: dateRangeValidator },
  );

  constructor() {
    this.loadHistory();
  }

  protected applyFilters(): void {
    this.requestError.set(null);

    if (this.filterForm.invalid) {
      this.filterForm.markAllAsTouched();
      return;
    }

    this.loadHistory(this.buildFilters());
  }

  protected clearFilters(): void {
    this.filterForm.reset();
    this.loadHistory();
  }

  protected hasDateRangeError(): boolean {
    return Boolean(
      this.filterForm.hasError('invalidDateRange') &&
        (this.filterForm.controls.fechaInicio.touched ||
          this.filterForm.controls.fechaFin.touched),
    );
  }

  protected formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? 'Fecha no disponible'
      : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
  }

  protected riskTone(level?: string | null): 'low' | 'medium' | 'high' | 'brand' {
    const normalized = (level ?? '').trim().toUpperCase();
    if (normalized === 'ALTO') return 'high';
    if (normalized === 'MEDIO') return 'medium';
    if (normalized === 'BAJO') return 'low';
    return 'brand';
  }

  protected loadHistory(filters?: AlertHistoryFilters): void {
    this.isLoading.set(true);
    this.requestError.set(null);

    this.alertCommunityService
      .getHistory(filters)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          // El backend acepta fechaInicio/fechaFin, pero algunas versiones no aplican ese rango.
          this.history.set(this.applyDateRangeFallback(response, filters));
        },
        error: (error: HttpErrorResponse) => {
          this.history.set([]);
          this.requestError.set(
            this.parseError(error, 'No fue posible cargar el historial de alertas.'),
          );
        },
      });
  }

  private buildFilters(): AlertHistoryFilters {
    const value = this.filterForm.getRawValue();

    return {
      tipoIncidente: value.tipoIncidente.trim() || undefined,
      estado: value.estado.trim() || undefined,
      fechaInicio: value.fechaInicio.trim() || undefined,
      fechaFin: value.fechaFin.trim() || undefined,
    };
  }

  private applyDateRangeFallback(
    items: AlertHistoryItem[],
    filters?: AlertHistoryFilters,
  ): AlertHistoryItem[] {
    const start = filters?.fechaInicio ? new Date(`${filters.fechaInicio}T00:00:00`) : null;
    const end = filters?.fechaFin ? new Date(`${filters.fechaFin}T23:59:59.999`) : null;
    const startTime = start && !Number.isNaN(start.getTime()) ? start.getTime() : null;
    const endTime = end && !Number.isNaN(end.getTime()) ? end.getTime() : null;

    if (startTime === null && endTime === null) {
      return items;
    }

    return items.filter((item) => {
      const time = this.dateTimeValue(item.fechaEmision);
      if (!Number.isFinite(time)) return false;
      if (startTime !== null && time < startTime) return false;
      return !(endTime !== null && time > endTime);
    });
  }

  private dateTimeValue(value: string): number {
    const date = new Date(value);
    return date.getTime();
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

    if (error.status === 400) return 'Revisa los filtros ingresados.';
    if (error.status === 401 || error.status === 403) return 'No tienes autorización para ver el historial.';

    return fallback;
  }
}
