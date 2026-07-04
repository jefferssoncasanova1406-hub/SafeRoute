import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';
import { RiskZoneService } from './risk-zone.service';

const DETAIL = {
  idZona: 7,
  tipo: 'Robo',
  nivelRiesgo: 3,
  descripcion: 'Zona reportada',
  estado: 'ACTIVA',
  fechaActualizacion: '2026-07-03T12:00:00',
  geometria: {
    type: 'Polygon',
    coordinates: [
      [
        [-77.04, -12.05],
        [-77.03, -12.05],
        [-77.04, -12.04],
      ],
    ],
  },
  ubicacion: { latitud: -12.05, longitud: -77.04, distrito: 'Lince', ciudad: 'Lima' },
};

describe('RiskZoneService fase 1', () => {
  let service: RiskZoneService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RiskZoneService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            buildAuthorizedHeaders: () => new HttpHeaders({ Authorization: 'Bearer test' }),
          },
        },
      ],
    });
    service = TestBed.inject(RiskZoneService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('consulta las alertas activas', () => {
    service.getActiveAlerts().subscribe((response) => expect(response.zonas[0]?.idZona).toBe(7));
    const request = httpMock.expectOne('http://localhost:8080/api/risk-zones/active-alerts');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test');
    request.flush({ message: 'OK', zonas: [DETAIL] });
  });

  it('consulta el detalle por identificador', () => {
    service.getRiskZoneDetail(7).subscribe((response) => expect(response.tipo).toBe('Robo'));
    const request = httpMock.expectOne('http://localhost:8080/api/risk-zones/7');
    expect(request.request.method).toBe('GET');
    request.flush(DETAIL);
  });

  it('envía coordenadas al comprobar proximidad', () => {
    service
      .checkProximity(-12.05, -77.04)
      .subscribe((response) => expect(response.alertaGenerada).toBeTruthy());
    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === 'http://localhost:8080/api/risk-zones/check-proximity' &&
        candidate.params.get('lat') === '-12.05' &&
        candidate.params.get('lon') === '-77.04',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ alertaGenerada: true, mensaje: 'Riesgo cercano', detalle: DETAIL });
  });

  it('permite una comprobación limitada sin coordenadas', () => {
    service.checkProximity().subscribe((response) => expect(response.alertaGenerada).toBeFalsy());
    const request = httpMock.expectOne('http://localhost:8080/api/risk-zones/check-proximity');
    expect(request.request.params.keys()).toEqual([]);
    request.flush({ alertaGenerada: false, mensaje: 'Evaluación limitada' });
  });
});
