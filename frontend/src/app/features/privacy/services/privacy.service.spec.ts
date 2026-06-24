import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpHeaders } from '@angular/common/http';

import { PrivacyService } from './privacy.service';
import { AuthService } from '../../../core/services/auth.service';
import { PrivacyPreferencesRequest, PrivacyPreferencesResponse } from '../models/privacy.model';

const MOCK_RESPONSE: PrivacyPreferencesResponse = {
  userId: 1,
  realTimeLocationEnabled: true,
  personalDataSharingEnabled: false,
  message: 'OK',
};

describe('PrivacyService', () => {
  let service: PrivacyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PrivacyService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { buildAuthorizedHeaders: () => new HttpHeaders({ Authorization: 'Bearer test' }) },
        },
      ],
    });
    service = TestBed.inject(PrivacyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getPreferences hace GET a /api/privacy/preferences', () => {
    let result: PrivacyPreferencesResponse | undefined;

    service.getPreferences().subscribe((res) => (result = res));

    const req = httpMock.expectOne('http://localhost:8080/api/privacy/preferences');
    expect(req.request.method).toBe('GET');
    req.flush(MOCK_RESPONSE);

    expect(result?.realTimeLocationEnabled).toBeTruthy();
    expect(result?.personalDataSharingEnabled).toBeFalsy();
    expect(result?.userId).toBe(1);
  });

  it('updatePreferences hace PUT con el payload correcto', () => {
    const payload: PrivacyPreferencesRequest = {
      realTimeLocationEnabled: false,
      personalDataSharingEnabled: true,
    };
    const mockPutResponse: PrivacyPreferencesResponse = {
      userId: 1,
      realTimeLocationEnabled: false,
      personalDataSharingEnabled: true,
      message: 'Actualizado',
    };
    let result: PrivacyPreferencesResponse | undefined;

    service.updatePreferences(payload).subscribe((res) => (result = res));

    const req = httpMock.expectOne('http://localhost:8080/api/privacy/preferences');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(payload);
    req.flush(mockPutResponse);

    expect(result?.realTimeLocationEnabled).toBeFalsy();
    expect(result?.personalDataSharingEnabled).toBeTruthy();
  });

  it('getPreferences incluye cabecera Authorization', () => {
    service.getPreferences().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/privacy/preferences');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test');
    req.flush(MOCK_RESPONSE);
  });
});
