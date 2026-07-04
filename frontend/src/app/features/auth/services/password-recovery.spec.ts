import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../../../core/services/auth.service';

describe('AuthService recuperación de contraseña', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('solicita recuperación usando el correo como parámetro', () => {
    service
      .requestPasswordReset('persona@correo.com')
      .subscribe((message) => expect(message).toContain('enlace'));
    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === 'http://localhost:8080/auth/forgot-password' &&
        candidate.params.get('email') === 'persona@correo.com',
    );
    expect(request.request.method).toBe('POST');
    request.flush('Se ha generado un enlace de recuperación.');
  });

  it('restablece la contraseña usando token y contraseña nueva', () => {
    service
      .resetPassword('token-123', 'NuevaClave123')
      .subscribe((message) => expect(message).toContain('restablecida'));
    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === 'http://localhost:8080/auth/reset-password' &&
        candidate.params.get('token') === 'token-123' &&
        candidate.params.get('newPassword') === 'NuevaClave123',
    );
    expect(request.request.method).toBe('POST');
    request.flush('Contraseña restablecida correctamente.');
  });
});
