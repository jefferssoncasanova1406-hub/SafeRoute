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
      {
        path: 'recuperar-contrasena',
        loadComponent: () =>
          import('./features/auth/pages/forgot-password/forgot-password').then(
            (module) => module.ForgotPasswordPage,
          ),
      },
      {
        path: 'restablecer-contrasena',
        loadComponent: () =>
          import('./features/auth/pages/reset-password/reset-password').then(
            (module) => module.ResetPasswordPage,
          ),
      },
    ],
  },
  {
    path: 'app',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'panel', pathMatch: 'full' },
      { path: 'panel', component: DashboardPage },
      {
        path: 'alertas/historial',
        loadComponent: () =>
          import('./features/alerts/pages/alert-history/alert-history').then(
            (module) => module.AlertHistoryPage,
          ),
      },
      {
        path: 'alertas/historial/:id',
        loadComponent: () =>
          import('./features/alerts/pages/alert-history-detail/alert-history-detail').then(
            (module) => module.AlertHistoryDetailPage,
          ),
      },
      {
        path: 'alertas',
        loadComponent: () =>
          import('./features/alerts/pages/alert-list/alert-list').then(
            (module) => module.AlertListPage,
          ),
      },
      {
        path: 'alertas/:id',
        loadComponent: () =>
          import('./features/alerts/pages/alert-detail/alert-detail').then(
            (module) => module.AlertDetailPage,
          ),
      },
      {
        path: 'rutas',
        loadComponent: () =>
          import('./features/routes/pages/route-calculate/route-calculate').then(
            (module) => module.RouteCalculatePage,
          ),
      },
      {
        path: 'perfil',
        loadComponent: () =>
          import('./features/profile/pages/profile-settings/profile-settings').then(
            (module) => module.ProfileSettingsPage,
          ),
      },
      {
        path: 'privacidad',
        loadComponent: () =>
          import('./features/privacy/pages/privacy-settings/privacy-settings').then(
            (module) => module.PrivacySettingsPage,
          ),
      },
      {
        path: 'zonas',
        loadComponent: () =>
          import('./features/risk-zones/map/pages/risk-zone-report/risk-zone-report').then(
            (module) => module.RiskZoneReportPage,
          ),
      },
      {
        path: 'reportar',
        loadComponent: () =>
          import('./features/alerts/pages/incident-report/incident-report').then(
            (module) => module.IncidentReportPage,
          ),
      },
    ],
  },
  {
    path: 'admin',
    component: AdminShell,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'zonas', pathMatch: 'full' },
      { path: 'zonas', component: RiskZoneManagementPage },
      {
        path: 'moderacion',
        loadComponent: () =>
          import('./features/alerts/pages/alert-moderation/alert-moderation').then(
            (module) => module.AlertModerationPage,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '/inicio' },
];
