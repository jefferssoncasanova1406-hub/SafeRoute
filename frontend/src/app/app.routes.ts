import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { PublicShell } from './layouts/public-shell/public-shell';
import { AppShell } from './layouts/app-shell/app-shell';
import { AdminShell } from './layouts/admin-shell/admin-shell';
import { LandingPage } from './features/landing/pages/landing/landing';
import { LoginPage } from './features/auth/pages/login/login';
import { AccountRegisterPage } from './features/auth/pages/account-register/account-register';
import { DashboardPage } from './features/dashboard/pages/dashboard/dashboard';
import { RouteCalculatePage } from './features/routes/pages/route-calculate/route-calculate';
import { RiskZoneReportPage } from './features/risk-zones/map/pages/risk-zone-report/risk-zone-report';
import { RiskZoneManagementPage } from './features/risk-zones/management/pages/risk-zone-management/risk-zone-management';

export const routes: Routes = [
  {
    path: '',
    component: PublicShell,
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: LandingPage },
      { path: 'iniciar-sesion', component: LoginPage },
      { path: 'crear-cuenta', component: AccountRegisterPage },
    ],
  },
  {
    path: 'app',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'panel', pathMatch: 'full' },
      { path: 'panel', component: DashboardPage },
      { path: 'rutas', component: RouteCalculatePage },
      { path: 'zonas', component: RiskZoneReportPage },
    ],
  },
  {
    path: 'admin',
    component: AdminShell,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'zonas', pathMatch: 'full' },
      { path: 'zonas', component: RiskZoneManagementPage },
    ],
  },
  { path: '**', redirectTo: '/inicio' },
];
