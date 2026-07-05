import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { PublicTrackingResponse } from '../../models/tracking.model';
import { TrackingService } from '../../services/tracking.service';
import { PublicTrackingPage } from './public-tracking';

vi.mock('mapbox-gl', () => {
  class MockMap {
    addControl(): void {}
    easeTo(): void {}
    remove(): void {}
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

  return {
    default: {
      Map: MockMap,
      Marker: MockMarker,
      NavigationControl: class {},
    },
  };
});

class TrackingServiceStub {
  response: Observable<PublicTrackingResponse> = of(TRACKING_RESPONSE);
  getPublicTracking = vi.fn(() => this.response);
}

describe('PublicTrackingPage', () => {
  let fixture: ComponentFixture<PublicTrackingPage>;
  let service: TrackingServiceStub;

  it('lee token desde la ruta y consulta seguimiento publico', async () => {
    await createComponent('abc12345');

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(service.getPublicTracking).toHaveBeenCalledWith('abc12345');
    expect(text).toContain('Franco');
    expect(text).toContain('En camino');
    expect(text).toContain('ubicaci');
    expect(text).toContain('-12.114200');
  });

  it('muestra token invalido sin consultar backend', async () => {
    await createComponent('@@@');

    expect(service.getPublicTracking).not.toHaveBeenCalled();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Token inv');
  });

  it('muestra error de enlace expirado', async () => {
    await createComponent(
      'expirado',
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            error: { message: 'El enlace de seguimiento ha expirado.' },
          }),
      ),
    );

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('expir');
  });

  it('muestra estado finalizado cuando el endpoint lo devuelve', async () => {
    await createComponent('abc12345', of({ ...TRACKING_RESPONSE, estadoRuta: 'FINALIZADA' }));

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Finalizada');
  });

  it('permite actualizacion manual', async () => {
    await createComponent('abc12345');

    clickButton(fixture.nativeElement as HTMLElement, 'Actualizar');
    fixture.detectChanges();

    expect(service.getPublicTracking).toHaveBeenCalledTimes(2);
  });

  async function createComponent(
    token: string,
    response: Observable<PublicTrackingResponse> = of(TRACKING_RESPONSE),
  ): Promise<void> {
    service = new TrackingServiceStub();
    service.response = response;

    await TestBed.configureTestingModule({
      imports: [PublicTrackingPage],
      providers: [
        provideRouter([]),
        { provide: TrackingService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ token }),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PublicTrackingPage);
    fixture.detectChanges();
  }
});

function clickButton(root: HTMLElement, label: string): void {
  const button = Array.from(root.querySelectorAll<HTMLButtonElement>('button')).find((item) =>
    (item.textContent ?? '').includes(label),
  );
  if (!button) throw new Error(`No se encontro el boton ${label}`);
  button.click();
}

const TRACKING_RESPONSE: PublicTrackingResponse = {
  nombreUsuario: 'Franco',
  latitudActual: -12.1142,
  longitudActual: -77.0234,
  ultimaActualizacion: 'Hace unos instantes',
  estadoRuta: 'EN_CAMINO',
};
