import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import mapboxgl from 'mapbox-gl';
import { finalize } from 'rxjs';

import { appRuntimeConfig } from '../../../../../core/config/runtime-config';
import { AuthService } from '../../../../../core/services/auth.service';
import { RiskZoneService } from '../../../../../core/services/risk-zone.service';
import { RiskZoneMapResponse, RiskZoneMapZone } from '../../../models/risk-zone.model';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

interface ZoneFeatureCollection {
  type: 'FeatureCollection';
  features: Array<{
    type: 'Feature';
    id: number;
    properties: { idZona: number; color: string; nivel: number };
    geometry: { type: 'Polygon'; coordinates: number[][][] };
  }>;
}

const ZONE_SOURCE_ID = 'active-risk-zones';
const ZONE_FILL_LAYER_ID = 'active-risk-zones-fill';
const ZONE_LINE_LAYER_ID = 'active-risk-zones-line';
const DEFAULT_CENTER: [number, number] = [-77.0428, -12.0464];
const EMPTY_ZONE_DATA: ZoneFeatureCollection = { type: 'FeatureCollection', features: [] };
const OSM_STYLE: mapboxgl.StyleSpecification = {
  version: 8,
  sources: {
    osm: {
      type: 'raster',
      tiles: [
        'https://a.tile.openstreetmap.org/{z}/{x}/{y}.png',
        'https://b.tile.openstreetmap.org/{z}/{x}/{y}.png',
        'https://c.tile.openstreetmap.org/{z}/{x}/{y}.png',
      ],
      tileSize: 256,
      attribution: '© OpenStreetMap contributors',
    },
  },
  layers: [{ id: 'osm', type: 'raster', source: 'osm' }],
};

@Component({
  selector: 'app-risk-zone-report-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './risk-zone-report.html',
  styleUrl: './risk-zone-report.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RiskZoneReportPage implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly riskZoneService = inject(RiskZoneService);

  @ViewChild('mapContainer')
  private mapContainer?: ElementRef<HTMLDivElement>;

  private map: mapboxgl.Map | null = null;
  private selectedFeatureId: number | null = null;

  protected readonly isAdmin = computed(() => this.auth.isAdminSession());
  protected readonly isLoading = signal(false);
  protected readonly requestError = signal<string | null>(null);
  protected readonly response = signal<RiskZoneMapResponse | null>(null);
  protected readonly selectedZoneId = signal<number | null>(null);
  protected readonly mapReady = signal(false);

  protected readonly filterForm = this.fb.nonNullable.group({ ciudad: [''], distrito: [''] });
  protected readonly zones = computed(() => this.response()?.zonas ?? []);
  protected readonly selectedZone = computed<RiskZoneMapZone | null>(() => {
    const zones = this.zones();
    return zones.find((zone) => zone.idZona === this.selectedZoneId()) ?? zones[0] ?? null;
  });
  protected readonly zoneMetrics = computed(() => {
    const zones = this.zones();
    return [
      { label: 'Total activas', value: zones.length.toString() },
      {
        label: 'Nivel alto',
        value: zones.filter((zone) => zone.nivelRiesgo >= 3).length.toString(),
      },
      {
        label: 'Nivel medio',
        value: zones.filter((zone) => zone.nivelRiesgo === 2).length.toString(),
      },
    ];
  });

  constructor() {
    this.loadZones();

    effect(() => {
      const zones = this.zones();
      if (this.mapReady()) this.renderZones(zones);
    });

    effect(() => {
      const selectedId = this.selectedZoneId();
      if (this.mapReady()) this.highlightZone(selectedId);
    });
  }

  ngAfterViewInit(): void {
    if (typeof window === 'undefined' || !this.mapContainer?.nativeElement) return;

    this.map = new mapboxgl.Map({
      accessToken: appRuntimeConfig.mapboxPublicToken,
      container: this.mapContainer.nativeElement,
      style: OSM_STYLE,
      center: DEFAULT_CENTER,
      zoom: 11.5,
      attributionControl: true,
    });
    this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');

    this.map.on('load', () => {
      if (!this.map) return;
      this.map.addSource(ZONE_SOURCE_ID, { type: 'geojson', data: EMPTY_ZONE_DATA as never });
      this.map.addLayer({
        id: ZONE_FILL_LAYER_ID,
        type: 'fill',
        source: ZONE_SOURCE_ID,
        paint: {
          'fill-color': ['get', 'color'],
          'fill-opacity': ['case', ['boolean', ['feature-state', 'selected'], false], 0.58, 0.3],
        },
      });
      this.map.addLayer({
        id: ZONE_LINE_LAYER_ID,
        type: 'line',
        source: ZONE_SOURCE_ID,
        paint: {
          'line-color': ['get', 'color'],
          'line-width': ['case', ['boolean', ['feature-state', 'selected'], false], 4, 2],
        },
      });
      this.map.on('click', ZONE_FILL_LAYER_ID, (event) => {
        const feature = event.features?.[0] as unknown as
          | { properties?: Record<string, unknown> }
          | undefined;
        const zoneId = Number(feature?.properties?.['idZona']);
        if (Number.isInteger(zoneId)) this.selectZone(zoneId);
      });
      this.map.on('mouseenter', ZONE_FILL_LAYER_ID, () => {
        if (this.map) this.map.getCanvas().style.cursor = 'pointer';
      });
      this.map.on('mouseleave', ZONE_FILL_LAYER_ID, () => {
        if (this.map) this.map.getCanvas().style.cursor = '';
      });
      this.mapReady.set(true);
    });

    this.map.on('error', (event) => console.error('Mapbox render error', event.error));
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  protected loadZones(): void {
    this.requestError.set(null);
    this.isLoading.set(true);
    const ciudad = this.filterForm.controls.ciudad.value.trim();
    const distrito = this.filterForm.controls.distrito.value.trim();

    this.riskZoneService
      .getActiveRiskZones({ ciudad: ciudad || undefined, distrito: distrito || undefined })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => {
          this.response.set(response);
          this.selectedZoneId.set(response.zonas[0]?.idZona ?? null);
        },
        error: (error: HttpErrorResponse) => {
          this.requestError.set(this.parseError(error, 'No fue posible cargar las zonas activas.'));
          this.response.set(null);
          this.selectedZoneId.set(null);
        },
      });
  }

  protected selectZone(zoneId: number): void {
    this.selectedZoneId.set(zoneId);
    const zone = this.zones().find((item) => item.idZona === zoneId);
    if (zone && this.map) {
      this.map.easeTo({
        center: [zone.centro.longitud, zone.centro.latitud],
        zoom: 14,
        duration: 500,
      });
    }
  }

  protected zoneTone(zone: RiskZoneMapZone): 'low' | 'medium' | 'high' {
    if (zone.nivelRiesgo >= 3) return 'high';
    if (zone.nivelRiesgo === 2) return 'medium';
    return 'low';
  }

  private renderZones(zones: RiskZoneMapZone[]): void {
    if (!this.map) return;
    const source = this.map.getSource(ZONE_SOURCE_ID) as mapboxgl.GeoJSONSource | undefined;
    if (!source) return;

    const features = zones
      .filter((zone) => (zone.geometria.coordinates[0]?.length ?? 0) >= 3)
      .map((zone) => ({
        type: 'Feature' as const,
        id: zone.idZona,
        properties: {
          idZona: zone.idZona,
          color: zone.color || this.fallbackColor(zone.nivelRiesgo),
          nivel: zone.nivelRiesgo,
        },
        geometry: { type: 'Polygon' as const, coordinates: zone.geometria.coordinates },
      }));
    source.setData({ type: 'FeatureCollection', features } as never);

    const bounds = new mapboxgl.LngLatBounds();
    features.forEach((feature) =>
      feature.geometry.coordinates[0]?.forEach((point) => bounds.extend([point[0], point[1]])),
    );
    if (!bounds.isEmpty()) this.map.fitBounds(bounds, { padding: 72, maxZoom: 14, duration: 600 });
    else this.map.easeTo({ center: DEFAULT_CENTER, zoom: 11.5 });
  }

  private highlightZone(zoneId: number | null): void {
    if (!this.map?.getSource(ZONE_SOURCE_ID)) return;
    if (this.selectedFeatureId !== null) {
      this.map.setFeatureState(
        { source: ZONE_SOURCE_ID, id: this.selectedFeatureId },
        { selected: false },
      );
    }
    if (zoneId !== null) {
      this.map.setFeatureState({ source: ZONE_SOURCE_ID, id: zoneId }, { selected: true });
    }
    this.selectedFeatureId = zoneId;
  }

  private fallbackColor(level: number): string {
    if (level >= 3) return '#b3362a';
    if (level === 2) return '#c17a0a';
    return '#1b7a52';
  }

  private parseError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) return 'No pudimos conectar con el servicio en este momento.';
    const body = error.error as ApiErrorBody | string | null;
    if (typeof body === 'string' && body.trim()) return body;
    if (body && typeof body === 'object') {
      const details = body.details ? Object.values(body.details).join(' ') : '';
      return details || body.message || fallback;
    }
    return fallback;
  }
}
