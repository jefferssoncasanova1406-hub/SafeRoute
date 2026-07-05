import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { AlertHistoryItem, ModerationRequest } from '../../models/alert-community.model';
import { AlertCommunityService } from '../../services/alert-community.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

type ModerationStatus = ModerationRequest['nuevoEstado'];

@Component({
  selector: 'app-alert-moderation-page',
  imports: [],
  templateUrl: './alert-moderation.html',
  styleUrl: './alert-moderation.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertModerationPage {
  private readonly alertCommunityService = inject(AlertCommunityService);

  protected readonly isLoading = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly operationMessage = signal<string | null>(null);
  protected readonly operationTone = signal<'success' | 'error' | null>(null);
  protected readonly pendingReports = signal<AlertHistoryItem[]>([]);
  protected readonly processingIds = signal<number[]>([]);

  constructor() {
    this.loadPendingReports();
  }

  protected loadPendingReports(): void {
    this.isLoading.set(true);
    this.loadError.set(null);

    this.alertCommunityService
      .getPendingReports()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.pendingReports.set(response),
        error: (error: HttpErrorResponse) => {
          this.pendingReports.set([]);
          this.loadError.set(
            this.parseError(error, 'No fue posible cargar los reportes pendientes.'),
          );
        },
      });
  }

  protected processReport(report: AlertHistoryItem, nuevoEstado: ModerationStatus): void {
    if (this.isProcessing(report.idAlerta)) {
      return;
    }

    if (!confirm(`Procesar el reporte ${report.idAlerta} como ${nuevoEstado}?`)) {
      return;
    }

    this.operationMessage.set(null);
    this.operationTone.set(null);
    this.processingIds.update((ids) => [...ids, report.idAlerta]);

    this.alertCommunityService
      .processModeration({ idIncidente: report.idAlerta, nuevoEstado })
      .pipe(
        finalize(() =>
          this.processingIds.update((ids) => ids.filter((id) => id !== report.idAlerta)),
        ),
      )
      .subscribe({
        next: (response) => {
          this.operationTone.set('success');
          this.operationMessage.set(response.message || 'El reporte fue procesado correctamente.');
          this.pendingReports.update((reports) =>
            reports.filter((item) => item.idAlerta !== report.idAlerta),
          );
        },
        error: (error: HttpErrorResponse) => {
          this.operationTone.set('error');
          this.operationMessage.set(
            this.parseError(error, 'No fue posible procesar el reporte seleccionado.'),
          );
        },
      });
  }

  protected isProcessing(id: number): boolean {
    return this.processingIds().includes(id);
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

  private parseError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) return 'No se pudo conectar con el servidor.';

    const body = error.error as ApiErrorBody | string | null;

    if (typeof body === 'string' && body.trim()) return body;

    if (body && typeof body === 'object') {
      const details = body.details ? Object.values(body.details).join(' ') : '';
      if (details) return details;
      if (body.message) return body.message;
    }

    if (error.status === 400) return 'La solicitud de moderación no pudo validarse.';
    if (error.status === 401 || error.status === 403) return 'No tienes autorización administrativa.';

    return fallback;
  }
}
