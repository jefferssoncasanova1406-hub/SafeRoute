import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AlertHistoryItem } from '../../models/alert-community.model';
import { AlertCommunityService } from '../../services/alert-community.service';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

@Component({
  selector: 'app-alert-history-detail-page',
  imports: [RouterLink],
  templateUrl: './alert-history-detail.html',
  styleUrl: './alert-history-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertHistoryDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly alertCommunityService = inject(AlertCommunityService);

  protected readonly isLoading = signal(true);
  protected readonly isVerifying = signal(false);
  protected readonly actionsLocked = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly verificationError = signal<string | null>(null);
  protected readonly verificationMessage = signal<string | null>(null);
  protected readonly alert = signal<AlertHistoryItem | null>(null);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.isLoading.set(false);
      this.requestError.set('El identificador del historial no es válido.');
      return;
    }

    this.loadDetail(id);
  }

  protected verifyIncident(verificado: boolean): void {
    const item = this.alert();
    if (!item || this.isVerifying() || this.actionsLocked()) {
      return;
    }

    this.verificationError.set(null);
    this.verificationMessage.set(null);
    this.isVerifying.set(true);

    this.alertCommunityService
      .verifyIncident({ idIncidente: item.idAlerta, verificado })
      .pipe(finalize(() => this.isVerifying.set(false)))
      .subscribe({
        next: (response) => {
          this.alert.set(response);
          this.actionsLocked.set(true);
          this.verificationMessage.set(
            response.message ||
              (verificado ? 'El reporte fue confirmado.' : 'El reporte fue rechazado.'),
          );
        },
        error: (error: HttpErrorResponse) => {
          this.verificationError.set(
            this.parseError(error, 'No fue posible registrar tu verificación.'),
          );
          if (error.status === 409) {
            this.actionsLocked.set(true);
          }
        },
      });
  }

  protected formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? 'Fecha no disponible'
      : new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' }).format(date);
  }

  protected riskTone(level?: string | null): 'low' | 'medium' | 'high' | 'brand' {
    const normalized = (level ?? '').trim().toUpperCase();
    if (normalized === 'ALTO') return 'high';
    if (normalized === 'MEDIO') return 'medium';
    if (normalized === 'BAJO') return 'low';
    return 'brand';
  }

  private loadDetail(id: number): void {
    this.alertCommunityService
      .getHistoryDetail(id)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (alert) => this.alert.set(alert),
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            this.parseError(error, 'No fue posible cargar el detalle histórico.'),
          );
        },
      });
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

    if (error.status === 400) return 'La solicitud no pudo validarse.';
    if (error.status === 404) return 'No se encontró el detalle para esta alerta histórica.';
    if (error.status === 409) return 'Ya registraste una verificación para este reporte.';
    if (error.status === 401 || error.status === 403) return 'No tienes autorización para esta acción.';

    return fallback;
  }
}
