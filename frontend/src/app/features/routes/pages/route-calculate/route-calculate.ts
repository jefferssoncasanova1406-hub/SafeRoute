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
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import mapboxgl from 'mapbox-gl';
import { finalize } from 'rxjs';

import { appRuntimeConfig } from '../../../../core/config/runtime-config';
import { RouteService } from '../../../../core/services/route.service';
import { RouteRequest, TransportMode } from '../../models/route-request.model';
import {
  ResolvedPlace,
  RouteOption,
  RouteResponse,
  RouteStep,
} from '../../models/route-response.model';

interface ApiErrorBody {
  error?: string;
  message?: string;
  details?: Record<string, string>;
}

interface TransportOption {
  value: TransportMode;
  label: string;
  note: string;
}

interface RouteMetric {
  label: string;
  value: string;
}

interface RouteFeatureProperties {
  routeId: string;
  summary: string;
}

interface RouteLineFeatureCollection {
  type: 'FeatureCollection';
  features: Array<{
    type: 'Feature';
    properties: RouteFeatureProperties;
    geometry: {
      type: 'LineString';
      coordinates: number[][];
    };
  }>;
}

const ROUTE_SOURCE_ID = 'evaluated-route';
const ROUTE_LAYER_ID = 'evaluated-route-line';
const DEFAULT_CENTER: [number, number] = [-77.0428, -12.0464];
const EMPTY_ROUTE_DATA: RouteLineFeatureCollection = {
  type: 'FeatureCollection',
  features: [],
};

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
  layers: [
    {
      id: 'osm',
      type: 'raster',
      source: 'osm',
    },
  ],
};

const DEMO_ROUTE: Pick<RouteRequest, 'origin' | 'destination' | 'transportMode'> = {
  origin: 'Av. Arequipa 2650, Lince',
  destination: 'Av. Javier Prado Este 4200, San Borja',
  transportMode: 'driving',
};

@Component({
  selector: 'app-route-calculate-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './route-calculate.html',
  styleUrl: './route-calculate.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteCalculatePage implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly routeService = inject(RouteService);

  @ViewChild('mapContainer')
  private mapContainer?: ElementRef<HTMLDivElement>;

  private map: mapboxgl.Map | null = null;
  private originMarker: mapboxgl.Marker | null = null;
  private destinationMarker: mapboxgl.Marker | null = null;

  protected readonly isSubmitting = signal(false);
  protected readonly routeResult = signal<RouteResponse | null>(null);
  protected readonly requestError = signal<string | null>(null);
  protected readonly selectedRouteId = signal<string | null>(null);
  protected readonly mapReady = signal(false);

  protected readonly transportOptions: TransportOption[] = [
    {
      value: 'driving',
      label: 'Auto',
      note: 'Vías principales y tiempos de tráfico de ruta.',
    },
    {
      value: 'walking',
      label: 'A pie',
      note: 'Recorridos urbanos aptos para peatones.',
    },
    {
      value: 'cycling',
      label: 'Bicicleta',
      note: 'Alternativas más cómodas para pedaleo.',
    },
  ];

  protected readonly routeForm = this.fb.nonNullable.group({
    origin: ['', [Validators.required, Validators.maxLength(200), this.requiredTrimmed]],
    destination: ['', [Validators.required, Validators.maxLength(200), this.requiredTrimmed]],
    transportMode: ['driving' as TransportMode, Validators.required],
  });

  protected readonly routeOptions = computed(() => this.routeResult()?.routes ?? []);

  protected readonly selectedRoute = computed(() => {
    const routes = this.routeOptions();
    const selectedRouteId = this.selectedRouteId();

    if (routes.length === 0) {
      return null;
    }

    return routes.find((route) => route.routeId === selectedRouteId) ?? routes[0] ?? null;
  });

  protected readonly selectedRouteMetrics = computed<RouteMetric[]>(() => {
    const route = this.selectedRoute();

    if (!route) {
      return [];
    }

    return [
      { label: 'Duración', value: this.formatDuration(route.durationMinutes) },
      { label: 'Distancia', value: this.formatDistance(route.distanceKm) },
      { label: 'Pasos', value: `${route.steps.length}` },
    ];
  });

  protected readonly selectedRouteSteps = computed(() => this.selectedRoute()?.steps ?? []);

  protected readonly resolvedOrigin = computed(() => this.routeResult()?.originResolved ?? null);
  protected readonly resolvedDestination = computed(
    () => this.routeResult()?.destinationResolved ?? null,
  );

  protected readonly transportModeLabel = computed(() =>
    this.formatTransportMode(this.routeResult()?.transportMode ?? this.routeForm.controls.transportMode.value),
  );

  constructor() {
    this.loadDemoRoute();

    effect(() => {
      const result = this.routeResult();
      const selectedRoute = this.selectedRoute();
      const mapReady = this.mapReady();

      if (!mapReady || !this.map) {
        return;
      }

      if (!result || !selectedRoute) {
        this.clearRenderedRoute();
        return;
      }

      this.renderRoute(selectedRoute, result.originResolved, result.destinationResolved);
    });
  }

  async ngAfterViewInit(): Promise<void> {
    if (typeof window === 'undefined' || !this.mapContainer?.nativeElement) {
      return;
    }

    this.map = new mapboxgl.Map({
      accessToken: appRuntimeConfig.mapboxPublicToken,
      container: this.mapContainer.nativeElement,
      style: OSM_STYLE,
      center: DEFAULT_CENTER,
      zoom: 11.8,
      attributionControl: true,
    });

    this.map.addControl(new mapboxgl.NavigationControl({ visualizePitch: true }), 'top-right');

    this.map.on('error', (event) => {
      console.error('Mapbox render error', event.error);
    });

    this.map.on('load', () => {
      if (!this.map || this.map.getSource(ROUTE_SOURCE_ID)) {
        this.mapReady.set(true);
        return;
      }

      this.map.addSource(ROUTE_SOURCE_ID, {
        type: 'geojson',
        data: EMPTY_ROUTE_DATA,
      });

      this.map.addLayer({
        id: ROUTE_LAYER_ID,
        type: 'line',
        source: ROUTE_SOURCE_ID,
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: {
          'line-color': '#0f6c78',
          'line-width': 6,
          'line-opacity': 0.9,
        },
      });

      this.mapReady.set(true);
    });
  }

  ngOnDestroy(): void {
    this.originMarker?.remove();
    this.destinationMarker?.remove();
    this.map?.remove();
  }

  protected loadDemoRoute(): void {
    this.routeForm.patchValue(DEMO_ROUTE);
    this.requestError.set(null);
  }

  protected selectTransportMode(mode: TransportMode): void {
    this.routeForm.controls.transportMode.setValue(mode);
    this.routeForm.controls.transportMode.markAsDirty();
  }

  protected calculateRoute(): void {
    this.requestError.set(null);

    if (this.routeForm.invalid) {
      this.routeForm.markAllAsTouched();
      return;
    }

    const payload: RouteRequest = {
      origin: this.routeForm.controls.origin.value.trim(),
      destination: this.routeForm.controls.destination.value.trim(),
      transportMode: this.routeForm.controls.transportMode.value,
    };

    this.isSubmitting.set(true);
    this.routeResult.set(null);
    this.selectedRouteId.set(null);

    this.routeService
      .evaluateRoute(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          if (!response.routes?.length) {
            this.requestError.set('El backend respondió sin rutas disponibles para ese trayecto.');
            return;
          }

          this.routeResult.set(response);
          this.selectedRouteId.set(response.routes[0]?.routeId ?? null);
        },
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            this.parseError(error, 'No fue posible calcular la ruta en este momento.'),
          );
        },
      });
  }

  protected selectRoute(routeId: string): void {
    this.selectedRouteId.set(routeId);
  }

  protected hasFieldError(controlName: 'origin' | 'destination'): boolean {
    const control = this.routeForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  protected fieldErrorMessage(controlName: 'origin' | 'destination'): string {
    const control = this.routeForm.controls[controlName];

    if (control.hasError('required') || control.hasError('requiredTrimmed')) {
      return controlName === 'origin' ? 'Ingresa un origen.' : 'Ingresa un destino.';
    }

    if (control.hasError('maxlength')) {
      return 'Máximo 200 caracteres.';
    }

    return 'Revisa este campo.';
  }

  protected isRouteSelected(routeId: string): boolean {
    return this.selectedRoute()?.routeId === routeId;
  }

  protected trackByRouteId(_: number, route: RouteOption): string {
    return route.routeId;
  }

  protected trackByStepOrder(_: number, step: RouteStep): number {
    return step.order;
  }

  protected formatDuration(minutes: number | null | undefined): string {
    if (minutes == null || Number.isNaN(minutes)) {
      return '--';
    }

    if (minutes < 60) {
      return `${Math.round(minutes)} min`;
    }

    const totalMinutes = Math.round(minutes);
    const hours = Math.floor(totalMinutes / 60);
    const remainingMinutes = totalMinutes % 60;

    if (remainingMinutes === 0) {
      return `${hours} h`;
    }

    return `${hours} h ${remainingMinutes} min`;
  }

  protected formatDistance(distanceKm: number | null | undefined): string {
    if (distanceKm == null || Number.isNaN(distanceKm)) {
      return '--';
    }

    return `${distanceKm.toFixed(1)} km`;
  }

  protected formatStepDistance(distanceMeters: number | null | undefined): string {
    if (distanceMeters == null || Number.isNaN(distanceMeters)) {
      return '--';
    }

    if (distanceMeters >= 1000) {
      return `${(distanceMeters / 1000).toFixed(1)} km`;
    }

    return `${Math.round(distanceMeters)} m`;
  }

  protected formatStepDuration(durationSeconds: number | null | undefined): string {
    if (durationSeconds == null || Number.isNaN(durationSeconds)) {
      return '--';
    }

    const minutes = Math.round(durationSeconds / 60);
    return minutes <= 1 ? '1 min' : `${minutes} min`;
  }

  protected formatTransportMode(mode: TransportMode): string {
    switch (mode) {
      case 'walking':
        return 'A pie';
      case 'cycling':
        return 'Bicicleta';
      default:
        return 'Auto';
    }
  }

  private renderRoute(
    route: RouteOption,
    originResolved: ResolvedPlace,
    destinationResolved: ResolvedPlace,
  ): void {
    if (!this.map) {
      return;
    }

    const source = this.map.getSource(ROUTE_SOURCE_ID) as mapboxgl.GeoJSONSource | undefined;
    const routeData = this.toRouteFeatureCollection(route);

    if (source) {
      source.setData(routeData as never);
    }

    this.originMarker?.remove();
    this.destinationMarker?.remove();

    this.originMarker = new mapboxgl.Marker({
      element: this.createMarkerElement('#0f6c78', 'O'),
      anchor: 'center',
    })
      .setLngLat([originResolved.longitude, originResolved.latitude])
      .addTo(this.map);

    this.destinationMarker = new mapboxgl.Marker({
      element: this.createMarkerElement('#cb7a1f', 'D'),
      anchor: 'center',
    })
      .setLngLat([destinationResolved.longitude, destinationResolved.latitude])
      .addTo(this.map);

    const bounds = new mapboxgl.LngLatBounds();

    route.geometry.coordinates.forEach((coordinate) => {
      bounds.extend([coordinate[0], coordinate[1]]);
    });

    bounds.extend([originResolved.longitude, originResolved.latitude]);
    bounds.extend([destinationResolved.longitude, destinationResolved.latitude]);

    if (!bounds.isEmpty()) {
      this.map.fitBounds(bounds, {
        padding: 64,
        duration: 700,
        maxZoom: 15,
      });
    }
  }

  private clearRenderedRoute(): void {
    if (!this.map) {
      return;
    }

    const source = this.map.getSource(ROUTE_SOURCE_ID) as mapboxgl.GeoJSONSource | undefined;

    if (source) {
      source.setData(EMPTY_ROUTE_DATA as never);
    }

    this.originMarker?.remove();
    this.destinationMarker?.remove();
    this.originMarker = null;
    this.destinationMarker = null;

    this.map.easeTo({
      center: DEFAULT_CENTER,
      zoom: 11.8,
      duration: 500,
    });
  }

  private toRouteFeatureCollection(route: RouteOption): RouteLineFeatureCollection {
    return {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: {
            routeId: route.routeId,
            summary: route.summary,
          },
          geometry: {
            type: 'LineString',
            coordinates: route.geometry.coordinates,
          },
        },
      ],
    };
  }

  private createMarkerElement(backgroundColor: string, label: string): HTMLDivElement {
    const element = document.createElement('div');
    element.style.width = '22px';
    element.style.height = '22px';
    element.style.borderRadius = '999px';
    element.style.background = backgroundColor;
    element.style.border = '3px solid #ffffff';
    element.style.boxShadow = '0 10px 20px rgba(20, 35, 49, 0.22)';
    element.style.display = 'grid';
    element.style.placeItems = 'center';
    element.style.color = '#ffffff';
    element.style.fontSize = '10px';
    element.style.fontWeight = '800';
    element.textContent = label;
    return element;
  }

  private requiredTrimmed(control: AbstractControl): { requiredTrimmed: true } | null {
    const value = control.value;

    if (typeof value === 'string' && value.trim().length === 0) {
      return { requiredTrimmed: true };
    }

    return null;
  }

  private parseError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) {
      return `No se pudo conectar con el backend. Verifica que esté escuchando en ${appRuntimeConfig.apiBaseUrl}.`;
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
