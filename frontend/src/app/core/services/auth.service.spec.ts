import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpHeaders } from '@angular/common/http';

import { AuthService } from './auth.service';

describe('AuthService.logout', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('logout hace POST a /auth/logout', () => {
    let result: { success: boolean; message: string } | undefined;

    service.logout().subscribe((res) => (result = res));

    const req = httpMock.expectOne('http://localhost:8080/auth/logout');
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'Sesión cerrada' });

    expect(result?.success).toBeTruthy();
    expect(result?.message).toBe('Sesión cerrada');
  });

  it('logout envía body vacío', () => {
    service.logout().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/auth/logout');
    expect(req.request.body).toEqual({});
    req.flush({ success: true, message: 'OK' });
  });
});
