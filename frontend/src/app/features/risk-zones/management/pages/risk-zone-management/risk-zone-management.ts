import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../../core/services/auth.service';
import { RiskZoneService } from '../../../../../core/services/risk-zone.service';
import {
  createBounds,
  toSvgPoints,
  type CoordinatePair,
} from '../../../../../shared/utils/geo-preview';
import { RiskZoneDetail, RiskZoneRequest } from '../../../models/risk-zone.model';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

const DEMO_ZONE = {
  tipo: 'Asalto recurrente',
  nivelRiesgo: '3',
  descripcion: 'Cruce con alta incidencia de robos nocturnos y baja visibilidad peatonal.',
  ciudad: 'Lima',
  distrito: 'Lince',
  centerLatitude: '-12.0918000',
  centerLongitude: '-77.0354000',
  latSpan: '0.0026',
  lngSpan: '0.0034',
};

@Component({
  selector: 'app-risk-zone-management-page',
  imports: [ReactiveFormsModule],
  templateUrl: './risk-zone-management.html',
  styleUrl: './risk-zone-management.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RiskZoneManagementPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly riskZoneService = inject(RiskZoneService);

  protected readonly isSaving = signal(false);
  protected readonly isLoading = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly operationMessage = signal<string | null>(null);
  protected readonly operationTone = signal<'success' | 'error' | null>(null);
  protected readonly zones = signal<RiskZoneDetail[]>([]);
  protected readonly editingZoneId = signal<number | null>(null);
  protected readonly isAdmin = computed(() => this.authService.isAdminSession());

  protected readonly selectedZone = computed(() => {
    const id = this.editingZoneId();
    return this.zones().find((zone) => zone.idZona === id) ?? null;
  });

  protected readonly selectedStateLabel = computed(
    () => this.selectedZone()?.estado ?? 'Nueva zona',
  );

  protected readonly filterForm = this.fb.nonNullable.group({
    estado: [''],
    nivelRiesgo: [''],
  });

  protected readonly zoneForm = this.fb.nonNullable.group({
    tipo: ['', [Validators.required, Validators.maxLength(50)]],
    nivelRiesgo: ['2', [Validators.required]],
    descripcion: ['', [Validators.required, Validators.maxLength(255)]],
    ciudad: ['', [Validators.required, Validators.maxLength(100)]],
    distrito: ['', [Validators.required, Validators.maxLength(100)]],
    centerLatitude: ['', [Validators.required, Validators.min(-90), Validators.max(90)]],
    centerLongitude: ['', [Validators.required, Validators.min(-180), Validators.max(180)]],
    latSpan: ['0.0025', [Validators.required, Validators.min(0.0001), Validators.max(1)]],
    lngSpan: ['0.0035', [Validators.required, Validators.min(0.0001), Validators.max(1)]],
  });

  protected readonly previewPolygon = computed(() => {
    const geometry = this.buildPolygonGeometry();

    if (!geometry) {
      return null;
    }

    const ring = geometry.coordinates[0].map(
      (coord) => [Number(coord[0]), Number(coord[1])] as CoordinatePair,
    );
    const bounds = createBounds([ring]);

    if (!bounds) {
      return null;
    }

    return toSvgPoints(ring, bounds, { padding: 18 });
  });

  constructor() {
    this.loadZones();
    this.loadDemoZone();
  }

  protected loadZones(): void {
    this.requestError.set(null);
    this.isLoading.set(true);

    const estado = this.filterForm.controls.estado.value.trim();
    const nivelRiesgoRaw = this.filterForm.controls.nivelRiesgo.value.trim();
    const nivelRiesgo = nivelRiesgoRaw ? Number(nivelRiesgoRaw) : undefined;

    this.riskZoneService
      .getRiskZones({ estado: estado || undefined, nivelRiesgo })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.zones.set(response.zonas);
        },
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            this.parseError(error, 'No fue posible cargar las zonas disponibles.'),
          );
          this.zones.set([]);
        },
      });
  }

  protected loadDemoZone(): void {
    this.zoneForm.patchValue(DEMO_ZONE);
  }

  protected editZone(zone: RiskZoneDetail): void {
    this.editingZoneId.set(zone.idZona);

    const ring = zone.geometria.coordinates[0] ?? [];
    const longitudes = ring.map((c) => Number(c[0]));
    const latitudes = ring.map((c) => Number(c[1]));
    const minLng = Math.min(...longitudes);
    const maxLng = Math.max(...longitudes);
    const minLat = Math.min(...latitudes);
    const maxLat = Math.max(...latitudes);

    this.zoneForm.patchValue({
      tipo: zone.tipo,
      nivelRiesgo: String(zone.nivelRiesgo),
      descripcion: zone.descripcion,
      ciudad: zone.ubicacion.ciudad,
      distrito: zone.ubicacion.distrito,
      centerLatitude: ((minLat + maxLat) / 2).toFixed(7),
      centerLongitude: ((minLng + maxLng) / 2).toFixed(7),
      latSpan: Math.max((maxLat - minLat) / 2, 0.0001).toFixed(4),
      lngSpan: Math.max((maxLng - minLng) / 2, 0.0001).toFixed(4),
    });
  }

  protected resetForm(): void {
    this.editingZoneId.set(null);
    this.zoneForm.reset({
      tipo: '',
      nivelRiesgo: '2',
      descripcion: '',
      ciudad: '',
      distrito: '',
      centerLatitude: '',
      centerLongitude: '',
      latSpan: '0.0025',
      lngSpan: '0.0035',
    });
  }

  protected saveZone(): void {
    this.operationMessage.set(null);
    this.operationTone.set(null);

    if (this.zoneForm.invalid) {
      this.zoneForm.markAllAsTouched();
      return;
    }

    const geometry = this.buildPolygonGeometry();

    if (!geometry) {
      this.operationTone.set('error');
      this.operationMessage.set(
        'No fue posible preparar el área con los datos ingresados.',
      );
      return;
    }

    const payload: RiskZoneRequest = {
      tipo: this.zoneForm.controls.tipo.value.trim(),
      nivelRiesgo: Number(this.zoneForm.controls.nivelRiesgo.value),
      descripcion: this.zoneForm.controls.descripcion.value.trim(),
      geometria: geometry,
      ubicacion: {
        ciudad: this.zoneForm.controls.ciudad.value.trim(),
        distrito: this.zoneForm.controls.distrito.value.trim(),
        latitud: Number(this.zoneForm.controls.centerLatitude.value),
        longitud: Number(this.zoneForm.controls.centerLongitude.value),
      },
    };

    this.isSaving.set(true);

    const operation$ = this.editingZoneId()
      ? this.riskZoneService.updateRiskZone(this.editingZoneId()!, payload)
      : this.riskZoneService.createRiskZone(payload);

    operation$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: (response) => {
        this.operationTone.set('success');
        this.operationMessage.set(response.message);
        this.loadZones();
        this.editZone(response.zona);
      },
      error: (error: HttpErrorResponse) => {
        this.operationTone.set('error');
        this.operationMessage.set(
          this.parseError(error, 'No fue posible guardar la zona en este momento.'),
        );
      },
    });
  }

  protected deactivateZone(zone: RiskZoneDetail): void {
    if (!confirm(`Desactivar la zona "${zone.tipo}" (ID ${zone.idZona})?`)) {
      return;
    }

    this.operationMessage.set(null);
    this.operationTone.set(null);
    this.isSaving.set(true);

    this.riskZoneService
      .deactivateRiskZone(zone.idZona)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (response) => {
          this.operationTone.set('success');
          this.operationMessage.set(response.message);
          this.loadZones();

          if (this.editingZoneId() === zone.idZona) {
            this.editZone(response.zona);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.operationTone.set('error');
          this.operationMessage.set(
            this.parseError(error, 'No fue posible desactivar la zona seleccionada.'),
          );
        },
      });
  }

  protected hasError(
    controlName:
      | 'tipo'
      | 'nivelRiesgo'
      | 'descripcion'
      | 'ciudad'
      | 'distrito'
      | 'centerLatitude'
      | 'centerLongitude'
      | 'latSpan'
      | 'lngSpan',
  ): boolean {
    const control = this.zoneForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private buildPolygonGeometry(): RiskZoneRequest['geometria'] | null {
    const lat = Number(this.zoneForm.controls.centerLatitude.value);
    const lng = Number(this.zoneForm.controls.centerLongitude.value);
    const latSpan = Number(this.zoneForm.controls.latSpan.value);
    const lngSpan = Number(this.zoneForm.controls.lngSpan.value);

    if (
      !Number.isFinite(lat) ||
      !Number.isFinite(lng) ||
      !Number.isFinite(latSpan) ||
      !Number.isFinite(lngSpan)
    ) {
      return null;
    }

    return {
      type: 'Polygon',
      coordinates: [
        [
          [Number((lng - lngSpan).toFixed(7)), Number((lat - latSpan).toFixed(7))],
          [Number((lng + lngSpan).toFixed(7)), Number((lat - latSpan).toFixed(7))],
          [Number((lng + lngSpan).toFixed(7)), Number((lat + latSpan).toFixed(7))],
          [Number((lng - lngSpan).toFixed(7)), Number((lat + latSpan).toFixed(7))],
          [Number((lng - lngSpan).toFixed(7)), Number((lat - latSpan).toFixed(7))],
        ],
      ],
    };
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
