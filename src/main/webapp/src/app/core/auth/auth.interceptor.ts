import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Read the token straight from storage instead of going through AuthService —
  // AuthService's constructor schedules an HTTP call, and resolving AuthService
  // here while it's still mid-construction is what triggered NG0200.
  const token = localStorage.getItem('authenticationToken');
  // Capture AuthService eagerly so we have a reference for the async error
  // handler — inject() is only legal in the synchronous interceptor body.
  const authService = inject(AuthService);

  // Always mark requests as AJAX so the backend (Spring Security) does not
  // emit a WWW-Authenticate: Basic header on 401 responses — which would
  // cause Chrome and Safari to pop up the browser's native credential dialog
  // instead of letting the Angular error handler redirect to /login.
  const headers: Record<string, string> = {
    'X-Requested-With': 'XMLHttpRequest',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  req = req.clone({ setHeaders: headers });

  return next(req).pipe(
    catchError((err) => {
      // Session expiry / unauthorised — log out and navigate to login.
      // Return EMPTY so the error does NOT propagate to the calling component,
      // which would otherwise show a toast or error message.
      // Skip the login endpoint itself so bad credentials still surface there.
      if (err.status === 401 && !req.url.endsWith('/api/authenticate')) {
        authService.logout();
        return EMPTY;
      }
      return throwError(() => err);
    }),
  );
};
