import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

interface DashboardAction {
  title: string;
  description: string;
  route: string;
  cta: string;
  tone: 'brand' | 'accent';
}

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private readonly auth = inject(AuthService);

  protected readonly session = this.auth.session;
  protected readonly isAdmin = computed(() => this.auth.isAdminSession());

  protected readonly firstName = computed(() => {
    const name = this.auth.session()?.user.nombre ?? '';
    const first = name.trim().split(' ')[0] ?? name;
    return first || 'Usuario';
  });

  protected readonly actions: DashboardAction[] = [
    {
      title: 'Planificar ruta',
      description:
        'Define tu trayecto, compara opciones y elige el recorrido que mejor se adapte a tu situación.',
      route: '/app/rutas',
      cta: 'Ir a rutas',
      tone: 'brand',
    },
    {
      title: 'Zonas activas',
      description:
        'Consulta el estado de las zonas en tu ciudad antes de moverte. Información organizada y clara.',
      route: '/app/zonas',
      cta: 'Ver zonas',
      tone: 'accent',
    },
  ];
}
