import { HttpErrorResponse } from '@angular/common/http';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import mapboxgl from 'mapbox-gl';
import { finalize } from 'rxjs';

import { appRuntimeConfig } from '../../../../core/config/runtime-config';
import { PublicTrackingResponse } from '../../models/tracking.model';
import { TrackingService } from '../../services/tracking.service';

interface ApiErrorBody {
  error?: string;
  message?: string;
  details?: Record<string, string>;
}

const DEFAULT_CENTER: [number, number] = [-77.0428, -12.0464];

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
      attribution: 'OpenStreetMap contributors',
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

@Component({
  selector: 'app-public-tracking-page',
  imports: [RouterLink],
  templateUrl: './public-tracking.html',
  styleUrl: './public-tracking.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicTrackingPage implements OnInit, AfterViewInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly trackingService = inject(TrackingService);

  @ViewChild('mapContainer')
  private mapContainer?: ElementRef<HTMLDivElement>;

  private map: mapboxgl.Map | null = null;
  private marker: mapboxgl.Marker | null = null;

  protected readonly token = signal<string | null>(null);
  protected readonly tracking = signal<PublicTrackingResponse | null>(null);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly mapReady = signal(false);
  protected readonly mapNotice = signal<string | null>(
    appRuntimeConfig.mapboxPublicToken
      ? null
      : 'El token pÃºblico de Mapbox no estÃ¡ configurado. La informaciÃ³n textual sigue disponible.',
  );

  protected readonly hasCoordinates = computed(() => {
    const tracking = this.tracking();
    return !!tracking && this.hasValidCoordinate(tracking.latitudActual, tracking.longitudActual);
  });

  protected readonly statusLabel = computed(() => this.formatStatus(this.tracking()?.estadoRuta));

  constructor() {
    effect(() => {
      const tracking = this.tracking();
      const mapReady = this.mapReady();

      if (!mapReady || !this.map || !tracking) {
        return;
      }

      this.renderLocation(tracking);
    });
  }

  ngOnInit(): void {
    const rawToken = this.route.snapshot.paramMap.get('token')?.trim() ?? '';

    if (!this.isValidToken(rawToken)) {
      this.errorMessage.set('Token invÃ¡lido. Revisa el enlace de seguimiento.');
      return;
    }

    this.token.set(rawToken);
    this.loadTracking();
  }

  ngAfterViewInit(): void {
    if (typeof window === 'undefined' || !this.mapContainer?.nativeElement) {
      return;
    }

    this.map = new mapboxgl.Map({
      accessToken: appRuntimeConfig.mapboxPublicToken,
      container: this.mapContainer.nativeElement,
      style: OSM_STYLE,
      center: DEFAULT_CENTER,
      zoom: 12,
      attributionControl: true,
    });

    this.map.addControl(new mapboxgl.NavigationControl(), 'top-right');
    this.map.on('error', (event) => console.error('Mapbox render error', event.error));
    this.map.on('load', () => this.mapReady.set(true));
  }

  ngOnDestroy(): void {
    this.marker?.remove();
    this.map?.remove();
  }

  protected loadTracking(): void {
    const token = this.token();

    if (!token || this.isLoading()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.trackingService
      .getPublicTracking(token)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.tracking.set(response),
        error: (error: HttpErrorResponse) => {
          this.tracking.set(null);
          this.errorMessage.set(this.parseTrackingError(error));
        },
      });
  }

  protected coordinateLabel(value: number | null | undefined): string {
    return typeof value === 'number' && Number.isFinite(value) ? value.toFixed(6) : 'No disponible';
  }

  private renderLocation(tracking: PublicTrackingResponse): void {
    if (!this.map || !this.hasValidCoordinate(tracking.latitudActual, tracking.longitudActual)) {
      return;
    }

    const latitude = tracking.latitudActual;
    const longitude = tracking.longitudActual;

    if (typeof latitude !== 'number' || typeof longitude !== 'number') {
      return;
    }

    const coordinates: [number, number] = [longitude, latitude];

    this.marker?.remove();
    this.marker = new mapboxgl.Marker({
      element: this.createMarkerElement(),
      anchor: 'center',
    })
      .setLngLat(coordinates)
      .addTo(this.map);

    this.map.easeTo({
      center: coordinates,
      zoom: 15,
      duration: 500,
    });
  }

  private createMarkerElement(): HTMLDivElement {
    const element = document.createElement('div');
    element.style.display = 'grid';
    element.style.placeItems = 'center';
    element.style.width = '2rem';
    element.style.height = '2rem';
    element.style.border = '3px solid #ffffff';
    element.style.borderRadius = '999px';
    element.style.background = '#0f6c78';
    element.style.boxShadow = '0 10px 20px rgba(20, 35, 49, 0.22)';
    element.style.color = '#ffffff';
    element.style.fontSize = '0.82rem';
    element.style.fontWeight = '800';
    element.textContent = 'U';
    element.setAttribute('aria-label', 'Ãšltima ubicaciÃ³n disponible');
    return element;
  }

  private hasValidCoordinate(
    latitude: number | null | undefined,
    longitude: number | null | undefined,
  ): boolean {
    return (
      typeof latitude === 'number' &&
      typeof longitude === 'number' &&
      Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude >= -90 &&
      latitude <= 90 &&
      longitude >= -180 &&
      longitude <= 180
    );
  }

  private isValidToken(token: string): boolean {
    return /^[A-Za-z0-9._-]{4,128}$/.test(token);
  }

  private formatStatus(status: string | null | undefined): string {
    if (!status) {
      return 'Estado no informado';
    }

    switch (status.trim().toUpperCase()) {
      case 'EN_CAMINO':
        return 'En camino';
      case 'FINALIZADA':
        return 'Finalizada';
      case 'EXPIRADO':
        return 'Expirado';
      case 'REVOCADO':
        return 'Revocado';
      default:
        return status;
    }
  }

  private parseTrackingError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return `No se pudo conectar con el backend en ${appRuntimeConfig.apiBaseUrl}.`;
    }

    const message = this.extractErrorMessage(error);
    const normalized = message.toLowerCase();

    if (error.status === 401 || error.status === 403) {
      return 'El backend no permite consultar este enlace pÃºblico sin autenticaciÃ³n.';
    }

    if (normalized.includes('expir')) {
      return 'El enlace de seguimiento expirÃ³.';
    }

    if (normalized.includes('revoc')) {
      return 'El enlace de seguimiento fue revocado.';
    }

    if (normalized.includes('finaliz')) {
      return 'El seguimiento finalizÃ³.';
    }

    if (error.status === 404) {
      return 'El enlace de seguimiento no existe o ya no estÃ¡ disponible.';
    }

    return message || 'No fue posible consultar el seguimiento.';
  }

  private extractErrorMessage(error: HttpErrorResponse): string {
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

      if (body.error) {
        return body.error;
      }
    }

    return '';
  }
}
