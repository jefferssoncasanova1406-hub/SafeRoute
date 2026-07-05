import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { RouteService } from '../../../../core/services/route.service';
import { RouteResponse, SafeRouteResponse } from '../../models/route-response.model';
import { RouteCalculatePage } from './route-calculate';

vi.mock('mapbox-gl', () => {
  class MockMap {
    private readonly source = { setData: vi.fn() };

    addControl(): void {}
    addSource(): void {}
    addLayer(): void {}
    fitBounds(): void {}
    easeTo(): void {}
    remove(): void {}
    getSource(): unknown {
      return this.source;
    }
    on(eventName: string, callback: () => void): void {
      if (eventName === 'load') {
        callback();
      }
    }
  }

  class MockMarker {
    setLngLat(): this {
      return this;
    }
    addTo(): this {
      return this;
    }
    remove(): void {}
  }

  class MockBounds {
    extend(): this {
      return this;
    }
    isEmpty(): boolean {
      return false;
    }
  }

  return {
    default: {
      Map: MockMap,
      Marker: MockMarker,
      NavigationControl: class {},
      LngLatBounds: MockBounds,
    },
  };
});

class RouteServiceStub {
  safeResponse: Observable<SafeRouteResponse> = of(SAFE_RESPONSE);
  evaluateRoute = vi.fn(() => of(ROUTE_RESPONSE));
  evaluateSafeRoute = vi.fn(() => this.safeResponse);
}


describe('RouteCalculatePage', () => {
  let fixture: ComponentFixture<RouteCalculatePage>;
  let routeService: RouteServiceStub;

  beforeEach(async () => {
    routeService = new RouteServiceStub();
    await TestBed.configureTestingModule({
      imports: [RouteCalculatePage],
      providers: [
        provideRouter([]),
        { provide: RouteService, useValue: routeService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RouteCalculatePage);
    fixture.detectChanges();
  });

  it('mantiene el calculo anterior y muestra distancia, tiempo e instrucciones', () => {
    submitForm(fixture.nativeElement as HTMLElement);
    fixture.detectChanges();

    expect(routeService.evaluateRoute).toHaveBeenCalledOnce();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('8.4 km');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('18 min');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Avanza por Javier Prado');
  });

  it('compara distancia, tiempo, seguridad y etiquetas', () => {
    submitForm(fixture.nativeElement as HTMLElement);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('ComparaciÃ³n de alternativas');
    expect(text).toContain('MÃ¡s rÃ¡pida');
    expect(text).toContain('MÃ¡s segura');
    expect(text).toContain('Recomendada');
    expect(text).toContain('Riesgo bajo');
    expect(text).toContain('Score 10');
    expect(text).toContain('Cruza 1 zona(s) de riesgo');
  });

  it('selecciona una alternativa y actualiza metricas e instrucciones', () => {
    submitForm(fixture.nativeElement as HTMLElement);
    fixture.detectChanges();

    const secondRoute = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      '.route-option-card',
    )[1];
    secondRoute.click();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ruta segura');
    expect(text).toContain('10.2 km');
    expect(text).toContain('24 min');
    expect(text).toContain('Continua por zona iluminada');
  });

  it('tolera datos de seguridad faltantes', () => {
    routeService.evaluateRoute = vi.fn(() => of(ROUTE_RESPONSE_WITHOUT_RISK));
    submitForm(fixture.nativeElement as HTMLElement);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Sin datos de riesgo');
  });

  it('mantiene rutas cuando falla solo la seguridad', () => {
    routeService.safeResponse = throwError(() => new HttpErrorResponse({ status: 500 }));
    submitForm(fixture.nativeElement as HTMLElement);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Ruta directa');
    expect(text).toContain('No fue posible cargar la evaluaciÃ³n de seguridad');
  });

});

function submitForm(root: HTMLElement): void {
  const form = root.querySelector<HTMLFormElement>('form');
  if (!form) throw new Error('No se encontro el formulario');
  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
}


const ROUTE_RESPONSE: RouteResponse = {
  originResolved: {
    name: 'Origen',
    address: 'Av. Arequipa 2650',
    latitude: -12.08,
    longitude: -77.04,
  },
  destinationResolved: {
    name: 'Destino',
    address: 'Av. Javier Prado 4200',
    latitude: -12.09,
    longitude: -76.99,
  },
  transportMode: 'driving',
  departureTime: null,
  routes: [
    {
      routeId: 'directa',
      summary: 'Ruta directa',
      durationMinutes: 18,
      distanceKm: 8.4,
      geometry: { type: 'LineString', coordinates: [[-77.04, -12.08], [-76.99, -12.09]] },
      steps: [
        {
          order: 1,
          instruction: 'Avanza por Javier Prado',
          streetName: 'Javier Prado',
          distanceMeters: 400,
          durationSeconds: 120,
          maneuverType: null,
          modifier: null,
        },
      ],
      nivelRiesgo: 'ALTO',
      scoreRiesgo: 80,
      cruzaZonasRiesgo: true,
      zonasRiesgo: [
        {
          idZona: 1,
          tipo: 'ROBO',
          nivelRiesgo: 3,
          nivelRiesgoNombre: 'Alto',
          color: null,
          descripcion: 'Zona reportada',
        },
      ],
    },
    {
      routeId: 'segura',
      summary: 'Ruta segura',
      durationMinutes: 24,
      distanceKm: 10.2,
      geometry: { type: 'LineString', coordinates: [[-77.04, -12.08], [-77.0, -12.08]] },
      steps: [
        {
          order: 1,
          instruction: 'Continua por zona iluminada',
          streetName: 'Av. Segura',
          distanceMeters: 800,
          durationSeconds: 200,
          maneuverType: null,
          modifier: null,
        },
      ],
      nivelRiesgo: 'BAJO',
      scoreRiesgo: 10,
      cruzaZonasRiesgo: false,
      zonasRiesgo: [],
    },
  ],
};

const ROUTE_RESPONSE_WITHOUT_RISK: RouteResponse = {
  ...ROUTE_RESPONSE,
  routes: ROUTE_RESPONSE.routes.map((route) => ({
    ...route,
    nivelRiesgo: null,
    scoreRiesgo: null,
    cruzaZonasRiesgo: null,
    zonasRiesgo: null,
  })),
};

const SAFE_RESPONSE: SafeRouteResponse = {
  origen: { latitud: -12.08, longitud: -77.04 },
  destino: { latitud: -12.09, longitud: -76.99 },
  rutaMasRapida: {
    distancia: 8400,
    tiempoEstimado: 18,
    scoreRiesgo: 80,
    nivelRiesgo: 'ALTO',
    cruzaZonasRiesgo: true,
    geometria: null,
    zonasRiesgo: [],
  },
  rutaMasSegura: {
    distancia: 10200,
    tiempoEstimado: 24,
    scoreRiesgo: 10,
    nivelRiesgo: 'BAJO',
    cruzaZonasRiesgo: false,
    geometria: null,
    zonasRiesgo: [],
  },
  rutaRecomendada: {
    distancia: 10200,
    tiempoEstimado: 24,
    scoreRiesgo: 10,
    nivelRiesgo: 'BAJO',
    cruzaZonasRiesgo: false,
    geometria: null,
    zonasRiesgo: [],
  },
  nivelRiesgo: 'BAJO',
  scoreRiesgo: 10,
  tiempoEstimado: 24,
  distancia: 10200,
  recomendacion: 'Se recomienda la ruta mas segura.',
};
