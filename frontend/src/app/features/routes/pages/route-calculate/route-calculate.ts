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

import { selectedMapStyle } from '../../../../shared/utils/map-style';
import { appRuntimeConfig } from '../../../../core/config/runtime-config';
import { RouteService } from '../../../../core/services/route.service';
import { PrivacyService } from '../../../privacy/services/privacy.service';
import { TrackingService } from '../../../tracking/services/tracking.service';
import { ShareTrackingResponse } from '../../../tracking/models/tracking.model';
import { RouteRequest, TransportMode } from '../../models/route-request.model';
import {
  ResolvedPlace,
  RouteOption,
  RouteResponse,
  SafeRouteOption,
  SafeRouteResponse,
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

interface RouteComparison {
  route: RouteOption;
  index: number;
  labels: string[];
}

interface SafeComparisonCard {
  label: string;
  option: SafeRouteOption;
}

interface RouteLegendItem {
  label: string;
  className: string;
}

interface RouteFeatureProperties {
  routeId: string;
  summary: string;
  riskLabel: string;
  color: string;
  selected: boolean;
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
  private readonly privacyService = inject(PrivacyService);
  private readonly trackingService = inject(TrackingService);

  @ViewChild('mapContainer')
  private mapContainer?: ElementRef<HTMLDivElement>;

  private map: mapboxgl.Map | null = null;
  private originMarker: mapboxgl.Marker | null = null;
  private destinationMarker: mapboxgl.Marker | null = null;
  private geolocationWatchId: number | null = null;
  private mapResizeObserver: ResizeObserver | null = null;

  protected readonly isSubmitting = signal(false);
  protected readonly isSecurityLoading = signal(false);
  protected readonly isSharing = signal(false);
  protected readonly isStoppingShare = signal(false);
  protected readonly routeResult = signal<RouteResponse | null>(null);
  protected readonly safeRouteResult = signal<SafeRouteResponse | null>(null);
  protected readonly requestError = signal<string | null>(null);
  protected readonly securityWarning = signal<string | null>(null);
  protected readonly shareMessage = signal<string | null>(null);
  protected readonly shareError = signal<string | null>(null);
  protected readonly activeTracking = signal<ShareTrackingResponse | null>(null);
  protected readonly trackingRevoked = signal(false);
  protected readonly selectedRouteId = signal<string | null>(null);
  protected readonly mapReady = signal(false);

  protected readonly routeLegend: RouteLegendItem[] = [
    { label: 'Riesgo bajo', className: 'badge--low' },
    { label: 'Riesgo medio', className: 'badge--medium' },
    { label: 'Riesgo alto', className: 'badge--high' },
  ];

  protected readonly transportOptions: TransportOption[] = [
    {
      value: 'driving',
      label: 'Auto',
      note: 'Vias principales y tiempos de trafico de ruta.',
    },
    {
      value: 'walking',
      label: 'A pie',
      note: 'Recorridos urbanos aptos para peatones.',
    },
    {
      value: 'cycling',
      label: 'Bicicleta',
      note: 'Alternativas mas comodas para pedaleo.',
    },
  ];

  protected readonly routeForm = this.fb.nonNullable.group({
    origin: ['', [Validators.required, Validators.maxLength(200), this.requiredTrimmed]],
    destination: ['', [Validators.required, Validators.maxLength(200), this.requiredTrimmed]],
    transportMode: ['driving' as TransportMode, Validators.required],
  });

  protected readonly routeOptions = computed(() => this.routeResult()?.routes ?? []);

  protected readonly publicTrackingLink = computed(() => {
    const token = this.activeTracking()?.tokenSeguimiento?.trim();

    if (!token || typeof window === 'undefined') {
      return null;
    }

    return `${window.location.origin}/seguimiento/${encodeURIComponent(token)}`;
  });

  protected readonly canShareLocation = computed(
    () =>
      !!this.routeResult() &&
      !!this.selectedRoute() &&
      !this.isSharing() &&
      !this.activeTracking(),
  );

  protected readonly canUsePublicLink = computed(
    () => !!this.publicTrackingLink() && !this.trackingRevoked() && !this.isStoppingShare(),
  );

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
      { label: 'Duracion', value: this.formatDuration(route.durationMinutes) },
      { label: 'Distancia', value: this.formatDistance(route.distanceKm) },
      { label: 'Seguridad', value: this.routeRiskLabel(route) },
      { label: 'Pasos', value: `${route.steps.length}` },
    ];
  });

  protected readonly selectedRouteSteps = computed(() => this.selectedRoute()?.steps ?? []);

  protected readonly routeComparisons = computed<RouteComparison[]>(() => {
    const routes = this.routeOptions();
    const fastestDuration = this.minimumDuration(routes);
    const safestRisk = this.minimumRiskScore(routes);
    const recommendedRouteId = this.recommendedRouteId(routes);

    return routes.map((route, index) => {
      const labels: string[] = [];

      if (fastestDuration !== null && route.durationMinutes === fastestDuration) {
        labels.push('Mas rapida');
      }

      if (safestRisk !== null && route.scoreRiesgo === safestRisk) {
        labels.push('Mas segura');
      }

      if (route.routeId === recommendedRouteId || route.recomendada === true) {
        labels.push('Recomendada');
      }

      return { route, index, labels };
    });
  });

  protected readonly safeComparisonCards = computed<SafeComparisonCard[]>(() => {
    const safeResult = this.safeRouteResult();

    if (!safeResult) {
      return [];
    }

    const cards: SafeComparisonCard[] = [];

    if (safeResult.rutaMasRapida) {
      cards.push({ label: 'Mas rapida', option: safeResult.rutaMasRapida });
    }

    if (safeResult.rutaMasSegura) {
      cards.push({ label: 'Mas segura', option: safeResult.rutaMasSegura });
    }

    if (safeResult.rutaRecomendada) {
      cards.push({ label: 'Recomendada', option: safeResult.rutaRecomendada });
    }

    return cards;
  });

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
      const selectedRouteId = this.selectedRouteId();
      const mapReady = this.mapReady();

      if (!mapReady || !this.map) {
        return;
      }

      if (!result || result.routes.length === 0) {
        this.clearRenderedRoutes();
        return;
      }

      this.renderRoutes(
        result.routes,
        selectedRouteId,
        result.originResolved,
        result.destinationResolved,
      );
    });
  }

  async ngAfterViewInit(): Promise<void> {
    if (typeof window === 'undefined' || !this.mapContainer?.nativeElement) {
      return;
    }

    this.map = new mapboxgl.Map({
      accessToken: appRuntimeConfig.mapboxPublicToken,
      container: this.mapContainer.nativeElement,
      style: selectedMapStyle(appRuntimeConfig.mapboxPublicToken),
      center: DEFAULT_CENTER,
      zoom: 11.8,
      attributionControl: true,
    });

    this.map.addControl(new mapboxgl.NavigationControl({ visualizePitch: true }), 'top-right');

    this.map.on('error', (event) => {
      console.error('Mapbox render error', event.error);
    });

        this.map.on('load', () => {
      this.ensureRouteLayer();
      this.mapReady.set(true);
      this.syncRenderedRoutes();
      this.resizeMap();
    });

    this.map.on('style.load', () => {
      this.ensureRouteLayer();
      this.syncRenderedRoutes();
      this.resizeMap();
    });

    this.attachMapResizeObserver();
  }

  ngOnDestroy(): void {
    this.stopLocationUpdates();
    this.mapResizeObserver?.disconnect();
    this.originMarker?.remove();
    this.destinationMarker?.remove();
    this.map?.remove();
  }

  protected loadDemoRoute(): void {
    this.routeForm.patchValue(DEMO_ROUTE);
    this.requestError.set(null);
    this.securityWarning.set(null);
    this.shareError.set(null);
    this.shareMessage.set(null);
  }

  protected selectTransportMode(mode: TransportMode): void {
    this.routeForm.controls.transportMode.setValue(mode);
    this.routeForm.controls.transportMode.markAsDirty();
  }

  protected calculateRoute(): void {
    this.requestError.set(null);
    this.securityWarning.set(null);
    this.shareError.set(null);
    this.shareMessage.set(null);

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
    this.safeRouteResult.set(null);
    this.activeTracking.set(null);
    this.trackingRevoked.set(false);
    this.selectedRouteId.set(null);

    this.routeService
      .evaluateRoute(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          if (!response.routes?.length) {
            this.requestError.set('El backend respondio sin rutas disponibles para ese trayecto.');
            return;
          }

          this.routeResult.set(response);
          this.selectedRouteId.set(
            response.routes.find((route) => route.recomendada === true)?.routeId ??
              response.routes[0]?.routeId ??
              null,
          );
          this.loadSafeRouteComparison(response);
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

  protected shareLocation(): void {
    if (!this.canShareLocation()) {
      return;
    }

    this.isSharing.set(true);
    this.shareError.set(null);
    this.shareMessage.set(null);

    this.privacyService.getPreferences().subscribe({
      next: (preferences) => {
        if (!preferences.realTimeLocationEnabled || !preferences.personalDataSharingEnabled) {
          this.isSharing.set(false);
          this.shareError.set(
            'Activa la ubicacion en tiempo real y el uso compartido de datos en Privacidad antes de compartir el seguimiento.',
          );
          return;
        }

        this.createTrackingLink();
      },
      error: (error: HttpErrorResponse) => {
        this.isSharing.set(false);
        this.shareError.set(
          this.parseError(error, 'No fue posible validar tu configuracion de privacidad.'),
        );
      },
    });
  }

  protected copyTrackingLink(): void {
    const link = this.publicTrackingLink();

    if (!link || !this.canUsePublicLink()) {
      return;
    }

    if (typeof navigator === 'undefined' || !navigator.clipboard) {
      this.shareError.set('Tu navegador no permite copiar el enlace automaticamente.');
      return;
    }

    navigator.clipboard
      .writeText(link)
      .then(() => {
        this.shareError.set(null);
        this.shareMessage.set('Enlace copiado al portapapeles.');
      })
      .catch(() => {
        this.shareError.set('No fue posible copiar el enlace. Seleccionalo manualmente.');
      });
  }

  protected shareTrackingLink(): void {
    const link = this.publicTrackingLink();

    if (!link || !this.canUsePublicLink()) {
      return;
    }

    if (typeof navigator === 'undefined' || !navigator.share) {
      this.shareError.set('Tu navegador no permite compartir directamente. Puedes copiar el enlace.');
      return;
    }

    navigator
      .share({
        title: 'Seguimiento SafeRoute',
        text: 'Sigue mi ultima ubicacion disponible en SafeRoute.',
        url: link,
      })
      .then(() => {
        this.shareError.set(null);
        this.shareMessage.set('Enlace enviado para compartir.');
      })
      .catch(() => {
        this.shareError.set('No se completo la accion de compartir.');
      });
  }

  protected stopTracking(): void {
    const token = this.activeTracking()?.tokenSeguimiento?.trim();

    if (!token || this.isStoppingShare()) {
      return;
    }

    this.isStoppingShare.set(true);
    this.shareError.set(null);
    this.shareMessage.set(null);

    this.trackingService
      .stop(token)
      .pipe(finalize(() => this.isStoppingShare.set(false)))
      .subscribe({
        next: (message) => {
          this.stopLocationUpdates();
          this.trackingRevoked.set(true);
          this.shareMessage.set(message || 'Seguimiento detenido.');
        },
        error: (error: HttpErrorResponse) => {
          this.shareError.set(
            this.parseError(error, 'No fue posible detener el seguimiento.'),
          );
        },
      });
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
      return 'Maximo 200 caracteres.';
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

  protected formatMetersAsKm(distanceMeters: number | null | undefined): string {
    if (distanceMeters == null || Number.isNaN(distanceMeters)) {
      return '--';
    }

    return `${(distanceMeters / 1000).toFixed(1)} km`;
  }

    protected routeRiskLabel(route: RouteOption): string {
    return this.normalizeRiskLabel(route.nivelRiesgo);
  }

  protected routeRiskBadgeClass(
    route: Pick<RouteOption, 'nivelRiesgo'> | Pick<SafeRouteOption, 'nivelRiesgo'> | null | undefined,
  ): string {
    const normalized = route?.nivelRiesgo?.trim().toLowerCase() ?? '';

    if (normalized.includes('alto')) {
      return 'badge--high';
    }

    if (normalized.includes('medio') || normalized.includes('moderado')) {
      return 'badge--medium';
    }

    if (normalized.includes('bajo')) {
      return 'badge--low';
    }

    return 'badge--brand';
  }

  protected safeRiskLabel(option: SafeRouteOption): string {
    return this.normalizeRiskLabel(option.nivelRiesgo);
  }

  protected riskScoreLabel(score: number | null | undefined): string {
    return score == null || Number.isNaN(score) ? 'Sin score' : `Score ${score}`;
  }

  protected crossedZonesLabel(
    crosses: boolean | null | undefined,
    zones: unknown[] | null | undefined,
  ): string {
    if (zones?.length) {
      return `Cruza ${zones.length} zona(s) de riesgo`;
    }

    if (crosses === true) {
      return 'Cruza zonas de riesgo';
    }

    if (crosses === false) {
      return 'No cruza zonas de riesgo';
    }

    return 'Sin datos de zonas';
  }

  protected formattedTrackingExpiration(): string {
    const value = this.activeTracking()?.fechaExpiracionEstimada;

    if (!value) {
      return 'Sin expiracion informada';
    }

    return value;
  }

  protected trackingStatusLabel(): string {
    if (this.trackingRevoked()) {
      return 'REVOCADO';
    }

    return this.activeTracking()?.estadoLink || 'ACTIVO';
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

  private loadSafeRouteComparison(response: RouteResponse): void {
    const origin = response.originResolved;
    const destination = response.destinationResolved;

    if (!this.hasValidCoordinate(origin.latitude, origin.longitude)) {
      this.securityWarning.set('No se pudo evaluar seguridad porque el origen no tiene coordenadas validas.');
      return;
    }

    if (!this.hasValidCoordinate(destination.latitude, destination.longitude)) {
      this.securityWarning.set('No se pudo evaluar seguridad porque el destino no tiene coordenadas validas.');
      return;
    }

    this.isSecurityLoading.set(true);

    this.routeService
      .evaluateSafeRoute({
        origen: {
          latitud: origin.latitude,
          longitud: origin.longitude,
        },
        destino: {
          latitud: destination.latitude,
          longitud: destination.longitude,
        },
      })
      .pipe(finalize(() => this.isSecurityLoading.set(false)))
      .subscribe({
        next: (safeResponse) => {
          this.safeRouteResult.set(safeResponse);
          this.securityWarning.set(null);
        },
        error: (error: HttpErrorResponse) => {
          this.safeRouteResult.set(null);
          this.securityWarning.set(
            this.parseError(error, 'No fue posible cargar la evaluacion de seguridad.'),
          );
        },
      });
  }

  private createTrackingLink(): void {
    this.trackingService
      .share()
      .pipe(finalize(() => this.isSharing.set(false)))
      .subscribe({
        next: (response) => {
          this.activeTracking.set(response);
          this.trackingRevoked.set(false);
          this.startLocationUpdates(response.tokenSeguimiento);
          this.shareMessage.set('Seguimiento activo. Ya puedes copiar o compartir el enlace.');
        },
        error: (error: HttpErrorResponse) => {
          this.shareError.set(
            this.parseError(error, 'No fue posible compartir la ubicacion.'),
          );
        },
      });
  }

  private startLocationUpdates(token: string): void {
    this.stopLocationUpdates();
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      this.shareError.set('El navegador no permite actualizar la ubicacion en tiempo real.');
      return;
    }
    this.geolocationWatchId = navigator.geolocation.watchPosition(
      position => {
        this.trackingService
          .updateLocation(token, position.coords.latitude, position.coords.longitude)
          .subscribe({ error: () => this.shareError.set('No fue posible actualizar la ubicacion compartida.') });
      },
      () => this.shareError.set('Se perdio el permiso de ubicacion durante el seguimiento.'),
      { enableHighAccuracy: true, maximumAge: 15000, timeout: 15000 },
    );
  }

  private stopLocationUpdates(): void {
    if (this.geolocationWatchId !== null && typeof navigator !== 'undefined') {
      navigator.geolocation.clearWatch(this.geolocationWatchId);
      this.geolocationWatchId = null;
    }
  }

  private minimumDuration(routes: RouteOption[]): number | null {
    const durations = routes
      .map((route) => route.durationMinutes)
      .filter((duration) => typeof duration === 'number' && !Number.isNaN(duration));

    return durations.length ? Math.min(...durations) : null;
  }

  private minimumRiskScore(routes: RouteOption[]): number | null {
    const scores = routes
      .map((route) => route.scoreRiesgo)
      .filter((score): score is number => typeof score === 'number' && !Number.isNaN(score));

    return scores.length ? Math.min(...scores) : null;
  }

  private recommendedRouteId(routes: RouteOption[]): string | null {
    return routes.find((route) => route.recomendada === true)?.routeId ?? routes[0]?.routeId ?? null;
  }

  private hasValidCoordinate(latitude: number, longitude: number): boolean {
    return (
      Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude >= -90 &&
      latitude <= 90 &&
      longitude >= -180 &&
      longitude <= 180
    );
  }

  private normalizeRiskLabel(value: string | null | undefined): string {
    if (!value?.trim()) {
      return 'Sin datos de riesgo';
    }

    const normalized = value.trim().toLowerCase();

    if (normalized.includes('alto')) {
      return 'Riesgo alto';
    }

    if (normalized.includes('medio') || normalized.includes('moderado')) {
      return 'Riesgo medio';
    }

    if (normalized.includes('bajo')) {
      return 'Riesgo bajo';
    }

    return value;
  }

    private syncRenderedRoutes(): void {
    const result = this.routeResult();

    if (!result || result.routes.length === 0) {
      this.clearRenderedRoutes();
      return;
    }

    this.renderRoutes(
      result.routes,
      this.selectedRouteId(),
      result.originResolved,
      result.destinationResolved,
    );
  }

  private renderRoutes(
    routes: RouteOption[],
    selectedRouteId: string | null,
    originResolved: ResolvedPlace,
    destinationResolved: ResolvedPlace,
  ): void {
    if (!this.map) {
      return;
    }

    const source = this.map.getSource(ROUTE_SOURCE_ID) as mapboxgl.GeoJSONSource | undefined;
    const routeData = this.toRouteFeatureCollection(
      routes,
      selectedRouteId,
      originResolved,
      destinationResolved,
    );

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

    routeData.features.forEach((feature) => {
      feature.geometry.coordinates.forEach((coordinate) => {
        bounds.extend([coordinate[0], coordinate[1]]);
      });
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

  private clearRenderedRoutes(): void {
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

  private toRouteFeatureCollection(
    routes: RouteOption[],
    selectedRouteId: string | null,
    originResolved: ResolvedPlace,
    destinationResolved: ResolvedPlace,
  ): RouteLineFeatureCollection {
    const orderedRoutes = [...routes].sort(
      (left, right) =>
        Number(left.routeId === selectedRouteId) - Number(right.routeId === selectedRouteId),
    );

    return {
      type: 'FeatureCollection',
      features: orderedRoutes.map((route) => {
        const coordinates = this.routeCoordinates(route, originResolved, destinationResolved);

        return {
          type: 'Feature',
          properties: {
            routeId: route.routeId,
            summary: route.summary,
            riskLabel: this.routeRiskLabel(route),
            color: this.routeLineColor(route.nivelRiesgo),
            selected: route.routeId === selectedRouteId,
          },
          geometry: {
            type: 'LineString',
            coordinates,
          },
        };
      }),
    };
  }

  private routeCoordinates(
    route: RouteOption,
    originResolved: ResolvedPlace,
    destinationResolved: ResolvedPlace,
  ): number[][] {
    return route.geometry.coordinates.length >= 2
      ? route.geometry.coordinates
      : [
          [originResolved.longitude, originResolved.latitude],
          [destinationResolved.longitude, destinationResolved.latitude],
        ];
  }

  private routeLineColor(riskLevel: string | null | undefined): string {
    const normalized = riskLevel?.trim().toLowerCase() ?? '';

    if (normalized.includes('alto')) {
      return '#dc2626';
    }

    if (normalized.includes('medio') || normalized.includes('moderado')) {
      return '#f59e0b';
    }

    if (normalized.includes('bajo')) {
      return '#22c55e';
    }

    return '#0f6c78';
  }

  private ensureRouteLayer(): void {
    if (!this.map) {
      return;
    }

    const mapWithOptionalApis = this.map as mapboxgl.Map & {
      getLayer?: (id: string) => unknown;
    };

    if (!this.map.getSource(ROUTE_SOURCE_ID)) {
      this.map.addSource(ROUTE_SOURCE_ID, {
        type: 'geojson',
        data: EMPTY_ROUTE_DATA,
      });
    }

    if (!mapWithOptionalApis.getLayer || !mapWithOptionalApis.getLayer(ROUTE_LAYER_ID)) {
      this.map.addLayer({
        id: ROUTE_LAYER_ID,
        type: 'line',
        source: ROUTE_SOURCE_ID,
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: {
          'line-color': ['get', 'color'] as never,
          'line-width': ['case', ['boolean', ['get', 'selected'], false], 8, 5] as never,
          'line-opacity': ['case', ['boolean', ['get', 'selected'], false], 0.98, 0.62] as never,
        },
      });
    }
  }

  private attachMapResizeObserver(): void {
    if (!this.mapContainer?.nativeElement || typeof ResizeObserver === 'undefined') {
      queueMicrotask(() => this.resizeMap());
      return;
    }

    this.mapResizeObserver?.disconnect();
    this.mapResizeObserver = new ResizeObserver(() => {
      this.resizeMap();
    });
    this.mapResizeObserver.observe(this.mapContainer.nativeElement);
    queueMicrotask(() => this.resizeMap());
  }

  private resizeMap(): void {
    const mapWithResize = this.map as (mapboxgl.Map & { resize?: () => void }) | null;
    mapWithResize?.resize?.();
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
      return `No se pudo conectar con el backend. Verifica que este escuchando en ${appRuntimeConfig.apiBaseUrl}.`;
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
