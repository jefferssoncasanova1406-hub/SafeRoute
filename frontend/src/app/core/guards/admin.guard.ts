import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAdminSession()) {
    return true;
  }

  return auth.session() !== null
    ? router.createUrlTree(['/app/panel'])
    : router.createUrlTree(['/iniciar-sesion']);
};
