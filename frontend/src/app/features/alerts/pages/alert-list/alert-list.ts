import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { RiskZoneService } from '../../../../core/services/risk-zone.service';
import { RiskZoneDetail } from '../../../risk-zones/models/risk-zone.model';

@Component({
  selector: 'app-alert-list-page',
  imports: [RouterLink],
  templateUrl: './alert-list.html',
  styleUrl: './alert-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertListPage {
  private readonly riskZoneService = inject(RiskZoneService);

  protected readonly isLoading = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly alerts = signal<RiskZoneDetail[]>([]);

  constructor() {
    this.loadAlerts();
  }

  protected loadAlerts(): void {
    this.isLoading.set(true);
    this.requestError.set(null);

    this.riskZoneService
      .getActiveAlerts()
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.alerts.set(response.zonas ?? []),
        error: (error: HttpErrorResponse) => {
          this.alerts.set([]);
          this.requestError.set(
            error.status === 0
              ? 'No se pudo conectar con el servicio de alertas.'
              : 'No fue posible cargar las alertas activas.',
          );
        },
      });
  }

  protected riskLabel(level: number): string {
    if (level >= 3) return 'Alto';
    if (level === 2) return 'Medio';
    return 'Bajo';
  }

  protected riskTone(level: number): 'low' | 'medium' | 'high' {
    if (level >= 3) return 'high';
    if (level === 2) return 'medium';
    return 'low';
  }

  protected formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime())
      ? 'Fecha no disponible'
      : new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
  }
}
