import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

export const roleGuard = (...roles: string[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasRole(...roles)) return true;
  return router.createUrlTree(['/app/dashboard']);
};
