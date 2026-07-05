import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { IncidentReportPage } from './incident-report';

describe('IncidentReportPage', () => {
  let fixture: ComponentFixture<IncidentReportPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncidentReportPage],
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

    fixture = TestBed.createComponent(IncidentReportPage);
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('marca campos obligatorios inválidos', () => {
    submitForm(fixture.nativeElement as HTMLElement);
    fixture.detectChanges();

    httpMock.expectNone('http://localhost:8080/api/alertas/reportar');
    expect(fixture.nativeElement.textContent).toContain('Ingresa un tipo de incidente válido');
    expect(fixture.nativeElement.textContent).toContain('Ingresa una ubicación aproximada');
  });

  it('rechaza descripción menor de 10 caracteres', () => {
    const root = fixture.nativeElement as HTMLElement;
    fillValidBase(root);
    setInput(root, '#report-description', 'corto');
    submitForm(root);
    fixture.detectChanges();

    httpMock.expectNone('http://localhost:8080/api/alertas/reportar');
    expect(root.textContent).toContain('entre 10 y 500 caracteres');
  });

  it('rechaza descripción mayor de 500 caracteres', () => {
    const root = fixture.nativeElement as HTMLElement;
    fillValidBase(root);
    setInput(root, '#report-description', 'a'.repeat(501));
    submitForm(root);
    fixture.detectChanges();

    httpMock.expectNone('http://localhost:8080/api/alertas/reportar');
    expect(root.textContent).toContain('entre 10 y 500 caracteres');
  });

  it('previene doble envío mientras la solicitud está en curso', () => {
    const root = fixture.nativeElement as HTMLElement;
    fillValidBase(root);
    submitForm(root);
    submitForm(root);

    const requests = httpMock.match('http://localhost:8080/api/alertas/reportar');
    expect(requests.length).toBe(1);
    requests[0].flush({
      idAlerta: 101,
      tipoIncidente: 'Robo',
      descripcion: 'Descripción válida',
      fechaEmision: '2026-07-04T10:00:00',
      estado: 'PENDIENTE',
      zonaAfectada: 'Lince',
      message: 'Registrado',
    });
  });

  it('envía body exacto y muestra éxito', () => {
    const root = fixture.nativeElement as HTMLElement;
    setInput(root, '#report-type', ' Robo ');
    setInput(root, '#report-location', ' Lince ');
    setInput(root, '#report-description', ' Descripción válida ');
    submitForm(root);

    const request = httpMock.expectOne('http://localhost:8080/api/alertas/reportar');
    expect(request.request.body).toEqual({
      tipoIncidente: 'Robo',
      ubicacion: 'Lince',
      descripcion: 'Descripción válida',
    });
    request.flush({
      idAlerta: 101,
      tipoIncidente: 'Robo',
      descripcion: 'Descripción válida',
      fechaEmision: '2026-07-04T10:00:00',
      estado: 'PENDIENTE',
      zonaAfectada: 'Lince',
      message: 'Reporte recibido',
    });
    fixture.detectChanges();

    expect(root.textContent).toContain('Reporte recibido');
    expect(root.querySelector<HTMLInputElement>('#report-type')?.value).toBe('');
  });

  it('muestra error del backend', () => {
    const root = fixture.nativeElement as HTMLElement;
    fillValidBase(root);
    submitForm(root);

    httpMock
      .expectOne('http://localhost:8080/api/alertas/reportar')
      .flush(
        { details: { descripcion: 'La descripción debe tener entre 10 y 500 caracteres' } },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();

    expect(root.textContent).toContain('La descripción debe tener entre 10 y 500 caracteres');
  });
});

function fillValidBase(root: HTMLElement): void {
  setInput(root, '#report-type', 'Robo');
  setInput(root, '#report-location', 'Lince');
  setInput(root, '#report-description', 'Descripción válida');
}

function setInput(root: HTMLElement, selector: string, value: string): void {
  const input = root.querySelector<HTMLInputElement | HTMLTextAreaElement>(selector);
  if (!input) throw new Error(`No se encontró ${selector}`);
  input.value = value;
  input.dispatchEvent(new Event('input'));
}

function submitForm(root: HTMLElement): void {
  const form = root.querySelector<HTMLFormElement>('form');
  if (!form) throw new Error('No se encontró el formulario');
  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
}
