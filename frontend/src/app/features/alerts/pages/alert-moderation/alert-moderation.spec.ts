import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthService } from '../../../../core/services/auth.service';
import { AlertHistoryItem } from '../../models/alert-community.model';
import { AlertModerationPage } from './alert-moderation';

const PENDING_REPORTS: AlertHistoryItem[] = [
  {
    idAlerta: 501,
    tipoIncidente: 'Robo',
    descripcion: 'Reporte pendiente',
    nivelRiesgo: 'ALTO',
    fechaEmision: '2026-07-04T10:00:00',
    estado: 'PENDIENTE',
    zonaAfectada: 'Lince',
  },
  {
    idAlerta: 502,
    tipoIncidente: 'Accidente',
    descripcion: 'Segundo reporte pendiente',
    fechaEmision: '2026-07-04T11:00:00',
    estado: 'PENDIENTE',
    zonaAfectada: 'Miraflores',
  },
];

describe('AlertModerationPage', () => {
  let fixture: ComponentFixture<AlertModerationPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertModerationPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
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

  afterEach(() => {
    vi.restoreAllMocks();
    httpMock.verify();
  });

  it('carga reportes pendientes', () => {
    fixture = createFixture();
    httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/pendientes').flush(PENDING_REPORTS);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Reporte pendiente');
  });

  it('muestra estado vacío', () => {
    fixture = createFixture();
    httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/pendientes').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay reportes pendientes');
  });

  it('procesa APROBADO y retira el elemento', () => {
    fixture = createLoadedFixture(httpMock);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    clickButton(fixture.nativeElement as HTMLElement, 'APROBADO');

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/procesar');
    expect(request.request.body).toEqual({ idIncidente: 501, nuevoEstado: 'APROBADO' });
    request.flush({ ...PENDING_REPORTS[0], estado: 'APROBADO', message: 'Aprobado' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aprobado');
    expect(fixture.nativeElement.textContent).not.toContain('Reporte pendiente');
    expect(fixture.nativeElement.textContent).toContain('Segundo reporte pendiente');
  });

  it('procesa RECHAZADO', () => {
    fixture = createLoadedFixture(httpMock);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    clickButton(fixture.nativeElement as HTMLElement, 'RECHAZADO');

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/procesar');
    expect(request.request.body).toEqual({ idIncidente: 501, nuevoEstado: 'RECHAZADO' });
    request.flush({ ...PENDING_REPORTS[0], estado: 'RECHAZADO', message: 'Rechazado' });
  });

  it('procesa FALSO', () => {
    fixture = createLoadedFixture(httpMock);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    clickButton(fixture.nativeElement as HTMLElement, 'FALSO');

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/procesar');
    expect(request.request.body).toEqual({ idIncidente: 501, nuevoEstado: 'FALSO' });
    request.flush({ ...PENDING_REPORTS[0], estado: 'FALSO', message: 'Falso' });
  });

  it('muestra error al procesar', () => {
    fixture = createLoadedFixture(httpMock);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    clickButton(fixture.nativeElement as HTMLElement, 'APROBADO');

    httpMock
      .expectOne('http://localhost:8080/api/alertas/moderacion/procesar')
      .flush({ message: 'No autorizado' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No autorizado');
    expect(fixture.nativeElement.textContent).toContain('Reporte pendiente');
  });
});

function createFixture(): ComponentFixture<AlertModerationPage> {
  const fixture = TestBed.createComponent(AlertModerationPage);
  fixture.detectChanges();
  return fixture;
}

function createLoadedFixture(httpMock: HttpTestingController): ComponentFixture<AlertModerationPage> {
  const fixture = createFixture();
  httpMock.expectOne('http://localhost:8080/api/alertas/moderacion/pendientes').flush(PENDING_REPORTS);
  fixture.detectChanges();
  return fixture;
}

function clickButton(root: HTMLElement, label: string): void {
  const button = [...root.querySelectorAll<HTMLButtonElement>('button')].find(
    (candidate) => candidate.textContent?.trim() === label,
  );
  if (!button) throw new Error(`No se encontró el botón ${label}`);
  button.click();
}
