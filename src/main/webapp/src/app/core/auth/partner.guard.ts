import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

const PARTNER_ROLES = [
  'ROLE_PARTNER_MAKER',
  'ROLE_PARTNER_CHECKER',
  'ROLE_PARTNER_VIEWER',
  'ROLE_ADMIN'
];

export const partnerGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const user = authService.getCurrentUser();
  if (user?.authorities?.some(role => PARTNER_ROLES.includes(role))) {
    return true;
  }

  router.navigate(['/home']);
  return false;
};
