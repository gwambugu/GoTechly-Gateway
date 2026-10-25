import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { PortalContextService } from '../services/portal-context.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const portalContext = inject(PortalContextService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // Admin features must only be reachable from the admin portal port.
  // The Gateway's PortAccessGlobalFilter rejects admin API calls (plugins,
  // services, configs) from the partner portal port with 403 — block the
  // route here so the user gets a clean redirect instead of dead pages.
  if (!portalContext.isAdminPortal()) {
    router.navigate(['/home']);
    return false;
  }

  const user = authService.getCurrentUser();
  if (user?.authorities?.includes('ROLE_ADMIN')) {
    return true;
  }

  router.navigate(['/home']);
  return false;
};
