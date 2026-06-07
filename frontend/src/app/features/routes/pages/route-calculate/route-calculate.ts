import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { RouteService } from '../../../../core/services/route.service';
import {
  createBounds,
  toSvgPoint,
  toSvgPoints,
  type CoordinatePair,
} from '../../../../shared/utils/geo-preview';
import { RouteRequest } from '../../models/route-request.model';
import { RouteOption, RouteResponse } from '../../models/route-response.model';

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

interface RouteCard {
  key: 'fastest' | 'safest' | 'recommended';
  label: string;
  title: string;
  note: string;
  tone: 'low' | 'medium' | 'high';
  option: RouteOption;
}

interface RoutePreviewLine {
  key: RouteCard['key'];
  points: string;
}

interface RoutePreview {
  lines: RoutePreviewLine[];
  origin: string;
  destination: string;
}

const DEMO_ROUTE = {
  originReference: 'Av. Arequipa 2650, Lince',
  originLatitude: '-12.0906000',
  originLongitude: '-77.0347000',
  destinationReference: 'Av. Javier Prado Este 4200, San Borja',
  destinationLatitude: '-12.0973000',
  destinationLongitude: '-76.9992000',
};

@Component({
  selector: 'app-route-calculate-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './route-calculate.html',
  styleUrl: './route-calculate.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteCalculatePage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly routeService = inject(RouteService);

  protected readonly isSubmitting = signal(false);
  protected readonly routeResult = signal<RouteResponse | null>(null);
  protected readonly requestError = signal<string | null>(null);

  protected readonly userName = computed(
    () => this.authService.session()?.user.nombre ?? '',
  );

  protected readonly routeForm = this.fb.nonNullable.group({
    originReference: [
      '',
      [Validators.required, Validators.maxLength(200), this.requiredTrimmed],
    ],
    originLatitude: ['', [Validators.required, Validators.min(-90), Validators.max(90)]],
    originLongitude: ['', [Validators.required, Validators.min(-180), Validators.max(180)]],
    destinationReference: [
      '',
      [Validators.required, Validators.maxLength(200), this.requiredTrimmed],
    ],
    destinationLatitude: ['', [Validators.required, Validators.min(-90), Validators.max(90)]],
    destinationLongitude: [
      '',
      [Validators.required, Validators.min(-180), Validators.max(180)],
    ],
  });

  protected readonly recommendedRoute = computed(() => this.routeResult()?.rutaRecomendada ?? null);

  protected readonly routeCards = computed<RouteCard[]>(() => {
    const result = this.routeResult();

    if (!result) {
      return [];
    }

    return [
      {
        key: 'fastest',
        label: 'Más rápida',
        title: 'Llega en menos tiempo',
        note: 'Pensada para reducir la duración total del recorrido.',
        tone: this.resolveRiskTone(result.rutaMasRapida.nivelRiesgo),
        option: result.rutaMasRapida,
      },
      {
        key: 'safest',
        label: 'Más tranquila',
        title: 'Reduce la exposición',
        note: 'Recorrido con menos cruces por áreas con actividad registrada.',
        tone: this.resolveRiskTone(result.rutaMasSegura.nivelRiesgo),
        option: result.rutaMasSegura,
      },
      {
        key: 'recommended',
        label: 'Recomendada',
        title: 'Equilibra tiempo y contexto',
        note: 'Combina duración y condiciones para sugerir la mejor opción.',
        tone: this.resolveRiskTone(result.rutaRecomendada.nivelRiesgo),
        option: result.rutaRecomendada,
      },
    ];
  });

  protected readonly routePreview = computed<RoutePreview | null>(() => {
    const result = this.routeResult();
    const cards = this.routeCards();

    if (!result || cards.length === 0) {
      return null;
    }

    const allGeometries = cards
      .map((card) => card.option.geometria?.coordinates ?? [])
      .filter((coords) => coords.length > 1)
      .map((coords) =>
        coords.map(
          (coord) => [Number(coord[0]), Number(coord[1])] as CoordinatePair,
        ),
      );

    const bounds = createBounds(allGeometries);

    if (!bounds) {
      return null;
    }

    return {
      origin: toSvgPoint([result.origen.longitud, result.origen.latitud], bounds),
      destination: toSvgPoint([result.destino.longitud, result.destino.latitud], bounds),
      lines: cards.map((card) => ({
        key: card.key,
        points: toSvgPoints(
          card.option.geometria.coordinates.map(
            (coord) => [Number(coord[0]), Number(coord[1])] as CoordinatePair,
          ),
          bounds,
        ),
      })),
    };
  });

  protected readonly recommendedRiskZones = computed(
    () => this.recommendedRoute()?.zonasRiesgo ?? [],
  );

  protected readonly routeHealth = computed(() => {
    const result = this.routeResult();

    if (!result) {
      return [];
    }

    return [
      { label: 'Distancia', value: `${result.distancia} m` },
      { label: 'Tiempo estimado', value: `${result.tiempoEstimado} min` },
      { label: 'Índice de riesgo', value: `${result.scoreRiesgo} / 100` },
    ];
  });

  constructor() {
    this.loadDemoRoute();
  }

  protected loadDemoRoute(): void {
    this.routeForm.patchValue(DEMO_ROUTE);
  }

  protected calculateRoute(): void {
    this.requestError.set(null);

    if (this.routeForm.invalid) {
      this.routeForm.markAllAsTouched();
      return;
    }

    const payload: RouteRequest = {
      origen: {
        latitud: Number(this.routeForm.controls.originLatitude.value),
        longitud: Number(this.routeForm.controls.originLongitude.value),
      },
      destino: {
        latitud: Number(this.routeForm.controls.destinationLatitude.value),
        longitud: Number(this.routeForm.controls.destinationLongitude.value),
      },
    };

    this.isSubmitting.set(true);
    this.routeResult.set(null);

    this.routeService
      .calculateRoute(payload)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (response) => {
          this.routeResult.set(response);
        },
        error: (error: HttpErrorResponse) => {
          this.requestError.set(
            this.parseError(error, 'No fue posible encontrar un trayecto en este momento.'),
          );
        },
      });
  }

  protected hasRouteError(
    controlName:
      | 'originReference'
      | 'originLatitude'
      | 'originLongitude'
      | 'destinationReference'
      | 'destinationLatitude'
      | 'destinationLongitude',
  ): boolean {
    const control = this.routeForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  protected routeTone(option: RouteOption | null): string {
    return this.resolveRiskTone(option?.nivelRiesgo ?? '');
  }

  protected riskLevelLabel(level: string): string {
    return level.trim() || 'bajo';
  }

  protected trackByRouteKey(_: number, card: RouteCard): string {
    return card.key;
  }

  private resolveRiskTone(riskLevel: string): 'low' | 'medium' | 'high' {
    const normalized = riskLevel.trim().toUpperCase();

    if (normalized.includes('ALTO')) {
      return 'high';
    }

    if (normalized.includes('MEDIO')) {
      return 'medium';
    }

    return 'low';
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
