import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { RiskZoneService } from '../../../../core/services/risk-zone.service';
import { RiskZoneDetail } from '../../../risk-zones/models/risk-zone.model';

@Component({
  selector: 'app-alert-detail-page',
  imports: [RouterLink],
  templateUrl: './alert-detail.html',
  styleUrl: './alert-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlertDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly riskZoneService = inject(RiskZoneService);

  protected readonly isLoading = signal(true);
  protected readonly requestError = signal<string | null>(null);
  protected readonly alert = signal<RiskZoneDetail | null>(null);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.isLoading.set(false);
      this.requestError.set('El identificador de la alerta no es válido.');
      return;
    }
    this.loadAlert(id);
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
      : new Intl.DateTimeFormat('es-PE', { dateStyle: 'long', timeStyle: 'short' }).format(date);
  }

  private loadAlert(id: number): void {
    this.riskZoneService
      .getRiskZoneDetail(id)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (alert) => this.alert.set(alert),
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            error.status === 404
              ? 'La alerta ya no está disponible o fue desactivada.'
              : 'No fue posible cargar el detalle de la alerta.',
          );
        },
      });
  }
}
