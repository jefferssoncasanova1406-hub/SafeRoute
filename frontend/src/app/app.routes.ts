import { Routes } from '@angular/router';
import { CalculateRoute } from './features/routes/calculate-route/calculate-route';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'calcular-ruta',
    pathMatch: 'full'
  },
  {
    path: 'calcular-ruta',
    component: CalculateRoute
  }
];
