import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { AlertHistoryItem } from '../../models/alert-community.model';
import { AlertHistoryDetailPage } from './alert-history-detail';

const DETAIL: AlertHistoryItem = {
  idAlerta: 101,
  tipoIncidente: 'Robo',
  descripcion: 'Reporte ciudadano',
  nivelRiesgo: 'MEDIO',
  fechaEmision: '2026-07-04T10:00:00',
  estado: 'PENDIENTE',
  zonaAfectada: 'Lince',
};

describe('AlertHistoryDetailPage', () => {
  let fixture: ComponentFixture<AlertHistoryDetailPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertHistoryDetailPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '101' }) } },
        },
        {
          provide: AuthService,
          useValue: {
            buildAuthorizedHeaders: () => new HttpHeaders({ Authorization: 'Bearer test' }),
          },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('confirmar envía verificado true', () => {
    fixture = createLoadedFixture(httpMock);
    clickButton(fixture.nativeElement as HTMLElement, 'Confirmar reporte');

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/verificar');
    expect(request.request.body).toEqual({ idIncidente: 101, verificado: true });
    request.flush({ ...DETAIL, estado: 'VERIFICADO', nivelRiesgo: 'ALTO', message: 'Confirmado' });
  });

  it('rechazar envía verificado false', () => {
    fixture = createLoadedFixture(httpMock);
    clickButton(fixture.nativeElement as HTMLElement, 'Rechazar reporte');

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/verificar');
    expect(request.request.body).toEqual({ idIncidente: 101, verificado: false });
    request.flush({ ...DETAIL, estado: 'RECHAZADO', message: 'Rechazado' });
  });

  it('bloquea acciones durante la solicitud', () => {
    fixture = createLoadedFixture(httpMock);
    const root = fixture.nativeElement as HTMLElement;
    clickButton(root, 'Confirmar reporte');
    fixture.detectChanges();

    const buttons = verificationButtons(root);
    expect(buttons.every((button) => button.disabled)).toBeTruthy();
    httpMock
      .expectOne('http://localhost:8080/api/alertas/verificar')
      .flush({ ...DETAIL, message: 'Confirmado' });
  });

  it('actualiza el detalle con la respuesta exitosa y bloquea acciones', () => {
    fixture = createLoadedFixture(httpMock);
    const root = fixture.nativeElement as HTMLElement;
    clickButton(root, 'Confirmar reporte');

    httpMock
      .expectOne('http://localhost:8080/api/alertas/verificar')
      .flush({ ...DETAIL, estado: 'VERIFICADO', nivelRiesgo: 'ALTO', message: 'Confirmado' });
    fixture.detectChanges();

    expect(root.textContent).toContain('Confirmado');
    expect(root.textContent).toContain('VERIFICADO');
    expect(root.textContent).toContain('ALTO');
    expect(verificationButtons(root).every((button) => button.disabled)).toBeTruthy();
  });

  it('muestra HTTP 409 y conserva el detalle visible', () => {
    fixture = createLoadedFixture(httpMock);
    const root = fixture.nativeElement as HTMLElement;
    clickButton(root, 'Confirmar reporte');

    httpMock
      .expectOne('http://localhost:8080/api/alertas/verificar')
      .flush(
        { message: 'Ya registraste una verificación para este reporte' },
        { status: 409, statusText: 'Conflict' },
      );
    fixture.detectChanges();

    expect(root.textContent).toContain('Ya registraste una verificación');
    expect(root.textContent).toContain('Reporte ciudadano');
    expect(verificationButtons(root).every((button) => button.disabled)).toBeTruthy();
  });
});

function createLoadedFixture(
  httpMock: HttpTestingController,
): ComponentFixture<AlertHistoryDetailPage> {
  const fixture = TestBed.createComponent(AlertHistoryDetailPage);
  fixture.detectChanges();
  httpMock.expectOne('http://localhost:8080/api/alertas/detalle/101').flush(DETAIL);
  fixture.detectChanges();
  return fixture;
}

function clickButton(root: HTMLElement, label: string): void {
  const button = [...root.querySelectorAll<HTMLButtonElement>('button')].find((candidate) =>
    candidate.textContent?.includes(label),
  );
  if (!button) throw new Error(`No se encontró el botón ${label}`);
  button.click();
}

function verificationButtons(root: HTMLElement): HTMLButtonElement[] {
  return [...root.querySelectorAll<HTMLButtonElement>('button')].filter((button) =>
    button.textContent?.includes('reporte'),
  );
}
