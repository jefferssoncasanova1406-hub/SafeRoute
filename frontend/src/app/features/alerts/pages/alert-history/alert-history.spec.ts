import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { AlertHistoryItem } from '../../models/alert-community.model';
import { AlertHistoryPage } from './alert-history';

const ALERTS: AlertHistoryItem[] = [
  {
    idAlerta: 101,
    tipoIncidente: 'Robo',
    descripcion: 'Reporte de robo',
    nivelRiesgo: 'ALTO',
    fechaEmision: '2026-07-04T10:00:00',
    estado: 'APROBADO',
    zonaAfectada: 'Lince',
  },
  {
    idAlerta: 102,
    tipoIncidente: 'Accidente',
    descripcion: 'Reporte de accidente',
    nivelRiesgo: 'MEDIO',
    fechaEmision: '2026-06-28T10:00:00',
    estado: 'APROBADO',
    zonaAfectada: 'San Isidro',
  },
];

describe('AlertHistoryPage', () => {
  let fixture: ComponentFixture<AlertHistoryPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertHistoryPage],
      providers: [
        provideRouter([]),
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

  afterEach(() => httpMock.verify());

  it('carga el historial inicial', () => {
    fixture = TestBed.createComponent(AlertHistoryPage);
    fixture.detectChanges();

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/historial');
    expect(request.request.method).toBe('GET');
    request.flush(ALERTS);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Reporte de robo');
  });

  it('envía filtros con contenido', () => {
    fixture = TestBed.createComponent(AlertHistoryPage);
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/alertas/historial').flush([]);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    setInput(root, '#history-type', 'Robo');
    setInput(root, '#history-state', 'APROBADO');
    setInput(root, '#history-start', '2026-07-01');
    setInput(root, '#history-end', '2026-07-04');
    submitForm(root);

    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === 'http://localhost:8080/api/alertas/historial' &&
        candidate.params.get('tipoIncidente') === 'Robo' &&
        candidate.params.get('estado') === 'APROBADO' &&
        candidate.params.get('fechaInicio') === '2026-07-01' &&
        candidate.params.get('fechaFin') === '2026-07-04',
    );
    request.flush([ALERTS[0]]);
    fixture.detectChanges();

    expect(root.textContent).toContain('Reporte de robo');
  });

  it('no consulta cuando el rango de fechas es inválido', () => {
    fixture = TestBed.createComponent(AlertHistoryPage);
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/alertas/historial').flush([]);

    const root = fixture.nativeElement as HTMLElement;
    setInput(root, '#history-start', '2026-07-04');
    setInput(root, '#history-end', '2026-07-01');
    submitForm(root);
    fixture.detectChanges();

    httpMock.expectNone(
      (candidate) => candidate.url === 'http://localhost:8080/api/alertas/historial',
    );
    expect(root.textContent).toContain('La fecha final no puede ser anterior');
  });

  it('limpia filtros y consulta sin parámetros', () => {
    fixture = TestBed.createComponent(AlertHistoryPage);
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/alertas/historial').flush([]);

    const root = fixture.nativeElement as HTMLElement;
    setInput(root, '#history-type', 'Robo');
    (fixture.componentInstance as unknown as ClearableHistoryPage).clearFilters();

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/historial');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([]);
  });

  it('muestra estado vacío', () => {
    fixture = TestBed.createComponent(AlertHistoryPage);
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/alertas/historial').flush([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay alertas históricas');
  });

  it('muestra error de carga', () => {
    fixture = TestBed.createComponent(AlertHistoryPage);
    fixture.detectChanges();
    httpMock
      .expectOne('http://localhost:8080/api/alertas/historial')
      .flush({ message: 'Error del servidor' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Error del servidor');
  });
});

interface ClearableHistoryPage {
  clearFilters(): void;
}

function setInput(root: HTMLElement, selector: string, value: string): void {
  const input = root.querySelector<HTMLInputElement>(selector);
  if (!input) throw new Error(`No se encontró ${selector}`);
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

function submitForm(root: HTMLElement): void {
  const form = root.querySelector<HTMLFormElement>('form');
  if (!form) throw new Error('No se encontró el formulario');
  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
}
