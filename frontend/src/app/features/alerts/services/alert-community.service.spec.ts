import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../../../core/services/auth.service';
import {
  AlertHistoryItem,
  CommunityVoteRequest,
  IncidentReportRequest,
  ModerationRequest,
} from '../models/alert-community.model';
import { AlertCommunityService } from './alert-community.service';

const ALERT: AlertHistoryItem = {
  idAlerta: 101,
  tipoIncidente: 'Robo',
  descripcion: 'Reporte ciudadano',
  nivelRiesgo: 'ALTO',
  fechaEmision: '2026-07-04T10:00:00',
  estado: 'PENDIENTE',
  zonaAfectada: 'Lince',
  message: 'OK',
};

describe('AlertCommunityService', () => {
  let service: AlertCommunityService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AlertCommunityService,
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
    service = TestBed.inject(AlertCommunityService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('consulta GET /api/alertas/historial con Authorization', () => {
    service.getHistory().subscribe((response) => expect(response[0]?.idAlerta).toBe(101));

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/historial');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test');
    request.flush([ALERT]);
  });

  it('envía solo query parameters no vacíos', () => {
    service
      .getHistory({
        tipoIncidente: ' Robo ',
        estado: '',
        fechaInicio: '2026-07-01',
        fechaFin: '2026-07-04',
      })
      .subscribe();

    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === 'http://localhost:8080/api/alertas/historial' &&
        candidate.params.get('tipoIncidente') === 'Robo' &&
        candidate.params.get('fechaInicio') === '2026-07-01' &&
        candidate.params.get('fechaFin') === '2026-07-04' &&
        !candidate.params.has('estado'),
    );
    expect(request.request.method).toBe('GET');
    request.flush([ALERT]);
  });

  it('consulta GET /api/alertas/detalle/{id}', () => {
    service.getHistoryDetail(101).subscribe((response) => expect(response.tipoIncidente).toBe('Robo'));

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/detalle/101');
    expect(request.request.method).toBe('GET');
    request.flush(ALERT);
  });

  it('envía POST /api/alertas/reportar con body exacto', () => {
    const payload: IncidentReportRequest = {
      tipoIncidente: 'Robo',
      ubicacion: 'Lince',
      descripcion: 'Descripción válida',
    };

    service.createReport(payload).subscribe((response) => expect(response.message).toBe('OK'));

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/reportar');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    expect(Object.keys(request.request.body as IncidentReportRequest)).toEqual([
      'tipoIncidente',
      'ubicacion',
      'descripcion',
    ]);
    request.flush(ALERT);
  });

  it('envía POST /api/alertas/verificar con verificado booleano', () => {
    const payload: CommunityVoteRequest = { idIncidente: 101, verificado: false };

    service.verifyIncident(payload).subscribe((response) => expect(response.idAlerta).toBe(101));

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/verificar');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    expect(typeof (request.request.body as CommunityVoteRequest).verificado).toBe('boolean');
    request.flush(ALERT);
  });

  it('consulta GET /api/alertas/moderacion/pendientes', () => {
    service.getPendingReports().subscribe((response) => expect(response.length).toBe(1));

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/pendientes');
    expect(request.request.method).toBe('GET');
    request.flush([ALERT]);
  });

  it('envía PUT /api/alertas/moderacion/procesar con body exacto', () => {
    const payload: ModerationRequest = { idIncidente: 501, nuevoEstado: 'APROBADO' };

    service.processModeration(payload).subscribe((response) => expect(response.message).toBe('OK'));

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/procesar');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    expect(Object.keys(request.request.body as ModerationRequest)).toEqual([
      'idIncidente',
      'nuevoEstado',
    ]);
    request.flush(ALERT);
  });
});
