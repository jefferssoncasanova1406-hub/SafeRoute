import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { RiskZoneService } from '../../../../core/services/risk-zone.service';
import { RiskProximityResponse } from '../../../risk-zones/models/risk-zone.model';

@Component({
  selector: 'app-nearby-risk-alert',
  imports: [RouterLink],
  templateUrl: './nearby-risk-alert.html',
  styleUrl: './nearby-risk-alert.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NearbyRiskAlert {
  private readonly riskZoneService = inject(RiskZoneService);

  protected readonly isChecking = signal(false);
  protected readonly result = signal<RiskProximityResponse | null>(null);
  protected readonly requestError = signal<string | null>(null);

  protected checkLocation(): void {
    this.requestError.set(null);
    this.result.set(null);

    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.checkWithoutCoordinates('Tu navegador no permite obtener la ubicación.');
      return;
    }

    this.isChecking.set(true);
    navigator.geolocation.getCurrentPosition(
      (position) => this.requestProximity(position.coords.latitude, position.coords.longitude),
      () => this.checkWithoutCoordinates('No se concedió acceso a la ubicación.'),
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 120000 },
    );
  }

  protected dismiss(): void {
    this.result.set(null);
    this.requestError.set(null);
  }

  private requestProximity(latitude: number, longitude: number): void {
    this.riskZoneService
      .checkProximity(latitude, longitude)
      .pipe(finalize(() => this.isChecking.set(false)))
      .subscribe({
        next: (response) => this.result.set(response),
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            error.status === 0
              ? 'No se pudo conectar con el servicio de proximidad.'
              : 'No fue posible comprobar los riesgos cercanos.',
          );
        },
      });
  }

  private checkWithoutCoordinates(reason: string): void {
    this.riskZoneService
      .checkProximity()
      .pipe(finalize(() => this.isChecking.set(false)))
      .subscribe({
        next: (response) => this.result.set({ ...response, mensaje: response.mensaje || reason }),
        error: () => this.requestError.set(reason),
      });
  }
}
