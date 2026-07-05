import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../../../core/services/auth.service';
import { TrackingService } from './tracking.service';

describe('TrackingService', () => {
  let service: TrackingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TrackingService,
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

    service = TestBed.inject(TrackingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('genera enlace de seguimiento sin body manual', () => {
    service.share().subscribe();

    const request = httpMock.expectOne('http://localhost:8080/api/tracking/compartir');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    expect(request.request.headers.get('Authorization')).toBe('Bearer test');
    request.flush({
      tokenSeguimiento: 'abc12345',
      urlCompleta: 'https://saferoute.pe/shared/tracking/abc12345',
      fechaExpiracionEstimada: '2026-07-05T12:00:00',
      estadoLink: 'ACTIVO',
    });
  });

  it('detiene seguimiento por token', () => {
    let response = '';

    service.stop('abc12345').subscribe((message) => (response = message));

    const request = httpMock.expectOne('http://localhost:8080/api/tracking/detener/abc12345');
    expect(request.request.method).toBe('PUT');
    request.flush('Revocado');

    expect(response).toBe('Revocado');
  });
});