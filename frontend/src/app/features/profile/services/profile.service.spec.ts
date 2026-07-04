import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../../../core/services/auth.service';
import { UpdateProfileRequest, UserProfile } from '../models/profile.model';
import { ProfileService } from './profile.service';

const PROFILE: UserProfile = {
  nombre: 'Ana Torres',
  email: 'ana@correo.com',
  preferenciasRiesg: 'medio',
  radioAlerta: 1.5,
  notificacionesActi: true,
};

describe('ProfileService', () => {
  let service: ProfileService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProfileService,
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
    service = TestBed.inject(ProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('carga el perfil autenticado', () => {
    service.getProfile().subscribe((profile) => expect(profile.email).toBe('ana@correo.com'));
    const request = httpMock.expectOne('http://localhost:8080/api/profile');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer test');
    request.flush(PROFILE);
  });

  it('actualiza datos y preferencias de movilidad', () => {
    const payload: UpdateProfileRequest = {
      nombre: 'Ana Torres',
      preferenciasRiesg: 'bajo',
      radioAlerta: 2,
      notificacionesActi: false,
    };
    service
      .updateProfile(payload)
      .subscribe((profile) => expect(profile.preferenciasRiesg).toBe('bajo'));
    const request = httpMock.expectOne('http://localhost:8080/api/profile');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(payload);
    request.flush({ ...PROFILE, ...payload });
  });

  it('cambia la contraseña con las credenciales indicadas', () => {
    const payload = { currentPassword: 'Anterior123', newPassword: 'NuevaClave123' };
    service
      .changePassword(payload)
      .subscribe((message) => expect(message).toContain('actualizada'));
    const request = httpMock.expectOne('http://localhost:8080/secure/change-password');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(payload);
    request.flush('Contraseña actualizada correctamente');
  });
});
