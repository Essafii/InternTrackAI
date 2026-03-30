import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allowed: string[] = route.data['roles'] ?? [];
  const user = auth.getCurrentUser();
  if (user && (allowed.length === 0 || allowed.includes(user.role))) return true;
  router.navigate(['/dashboard']);
  return false;
};
