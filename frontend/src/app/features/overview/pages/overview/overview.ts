import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface FlowStep {
  order: string;
  title: string;
  copy: string;
  route: string;
  cta: string;
}

interface ModuleCard {
  title: string;
  badge: string;
  copy: string;
  route: string;
}

@Component({
  selector: 'app-overview-page',
  imports: [CommonModule, RouterLink],
  templateUrl: './overview.html',
  styleUrl: './overview.scss',
})
export class OverviewPage {
  protected readonly flow: FlowStep[] = [
    {
      order: '01',
      title: 'Empieza con tu cuenta',
      copy: 'Configura el acceso inicial y deja lista tu informacion antes de usar los demas modulos.',
      route: '/registro-cuenta',
      cta: 'Crear cuenta',
    },
    {
      order: '02',
      title: 'Organiza tu trayecto',
      copy: 'Compara opciones de recorrido y elige la que mejor se adapte al tiempo y al nivel de exposicion.',
      route: '/rutas-seguras',
      cta: 'Ver rutas',
    },
    {
      order: '03',
      title: 'Consulta el mapa de zonas',
      copy: 'Revisa las areas activas de un vistazo para entender mejor el contexto antes de moverte.',
      route: '/zonas-operativas',
      cta: 'Ver zonas',
    },
    {
      order: '04',
      title: 'Administra la informacion',
      copy: 'Actualiza, corrige o desactiva zonas desde un solo panel de gestion cuando tengas permisos.',
      route: '/zonas-administracion',
      cta: 'Abrir gestion',
    },
  ];

  protected readonly modules: ModuleCard[] = [
    {
      title: 'Cuenta',
      badge: 'Acceso',
      copy: 'Alta inicial y continuidad de uso.',
      route: '/registro-cuenta',
    },
    {
      title: 'Rutas',
      badge: 'Trayectos',
      copy: 'Comparacion entre tiempo, seguridad y sugerencia final.',
      route: '/rutas-seguras',
    },
    {
      title: 'Mapa',
      badge: 'Zonas',
      copy: 'Lectura visual de las zonas activas en la ciudad.',
      route: '/zonas-operativas',
    },
    {
      title: 'Gestion',
      badge: 'Control',
      copy: 'Edicion y control del estado de las zonas.',
      route: '/zonas-administracion',
    },
  ];
}
