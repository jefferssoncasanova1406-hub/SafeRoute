import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { RiskZoneService } from '../../../../../core/services/risk-zone.service';
import {
  createBounds,
  toSvgPoint,
  toSvgPoints,
  type CoordinatePair,
} from '../../../../../shared/utils/geo-preview';
import { RiskZoneMapResponse, RiskZoneMapZone } from '../../../models/risk-zone.model';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

interface AtlasPolygon {
  idZona: number;
  points: string;
  color: string;
}

interface AtlasPreview {
  polygons: AtlasPolygon[];
  centers: Array<{ idZona: number; point: string }>;
}

@Component({
  selector: 'app-risk-zone-report-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './risk-zone-report.html',
  styleUrl: './risk-zone-report.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RiskZoneReportPage {
  private readonly fb = inject(FormBuilder);
  private readonly riskZoneService = inject(RiskZoneService);

  protected readonly isLoading = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly response = signal<RiskZoneMapResponse | null>(null);
  protected readonly selectedZoneId = signal<number | null>(null);

  protected readonly filterForm = this.fb.nonNullable.group({
    ciudad: [''],
    distrito: [''],
  });

  protected readonly zones = computed(() => this.response()?.zonas ?? []);

  protected readonly selectedZone = computed(() => {
    const zones = this.zones();

    if (zones.length === 0) {
      return null;
    }

    return zones.find((zone) => zone.idZona === this.selectedZoneId()) ?? zones[0];
  });

  protected readonly zoneMetrics = computed(() => {
    const zones = this.zones();

    return [
      { label: 'Total activas', value: zones.length.toString() },
      {
        label: 'Nivel alto',
        value: zones.filter((z) => z.nivelRiesgo === 3).length.toString(),
      },
      {
        label: 'Nivel medio',
        value: zones.filter((z) => z.nivelRiesgo === 2).length.toString(),
      },
    ];
  });

  protected readonly atlasPreview = computed<AtlasPreview | null>(() => {
    const zones = this.zones();
    const rings = zones
      .map((zone) => zone.geometria.coordinates[0] ?? [])
      .filter((ring) => ring.length > 2)
      .map((ring) =>
        ring.map(
          (coord) => [Number(coord[0]), Number(coord[1])] as CoordinatePair,
        ),
      );

    const bounds = createBounds(rings);

    if (!bounds) {
      return null;
    }

    return {
      polygons: zones.map((zone) => ({
        idZona: zone.idZona,
        points: toSvgPoints(
          (zone.geometria.coordinates[0] ?? []).map(
            (coord) => [Number(coord[0]), Number(coord[1])] as CoordinatePair,
          ),
          bounds,
        ),
        color: zone.color,
      })),
      centers: zones.map((zone) => ({
        idZona: zone.idZona,
        point: toSvgPoint([Number(zone.centro.longitud), Number(zone.centro.latitud)], bounds),
      })),
    };
  });

  constructor() {
    this.loadZones();
  }

  protected loadZones(): void {
    this.requestError.set(null);
    this.isLoading.set(true);

    const ciudad = this.filterForm.controls.ciudad.value.trim();
    const distrito = this.filterForm.controls.distrito.value.trim();

    this.riskZoneService
      .getActiveRiskZones({
        ciudad: ciudad || undefined,
        distrito: distrito || undefined,
      })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.response.set(response);
          this.selectedZoneId.set(response.zonas[0]?.idZona ?? null);
        },
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            this.parseError(error, 'No fue posible cargar las zonas activas.'),
          );
          this.response.set(null);
        },
      });
  }

  protected selectZone(zoneId: number): void {
    this.selectedZoneId.set(zoneId);
  }

  protected zoneTone(zone: RiskZoneMapZone): 'low' | 'medium' | 'high' {
    if (zone.nivelRiesgo >= 3) {
      return 'high';
    }

    if (zone.nivelRiesgo === 2) {
      return 'medium';
    }

    return 'low';
  }

  private parseError(error: HttpErrorResponse, fallback: string): string {
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

    return fallback;
  }
}
