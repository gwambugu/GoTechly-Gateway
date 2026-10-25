import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export type PortalRole = 'ADMIN' | 'PARTNER' | 'OTHER';

interface PortalContextResponse {
  role: PortalRole;
  port: number;
}

/**
 * Resolves which logical portal (admin / partner / other) the SPA is running
 * under by asking the Gateway. The Gateway is the single source of truth for
 * port → role mapping (gotechly.gateway.multi-port.* in application.yml), so
 * we don't hardcode port numbers in the SPA anymore.
 *
 * Bootstrapped via provideAppInitializer in app.config.ts so the role is
 * known before any component renders.
 */
@Injectable({
  providedIn: 'root',
})
export class PortalContextService {
  private http = inject(HttpClient);
  private role: PortalRole = 'OTHER';
  private port = -1;

  load(): Promise<void> {
    return firstValueFrom(this.http.get<PortalContextResponse>('/api/portal-context'))
      .then((res) => {
        this.role = res.role;
        this.port = res.port;
      })
      .catch((err) => {
        console.error('Failed to resolve portal context — defaulting to OTHER:', err);
        this.role = 'OTHER';
      });
  }

  getRole(): PortalRole {
    return this.role;
  }

  isAdminPortal(): boolean {
    return this.role === 'ADMIN';
  }

  isPartnerPortal(): boolean {
    return this.role === 'PARTNER';
  }

  /** Browser-reported port — kept for diagnostics/logging only, not for role decisions. */
  getCurrentPort(): string {
    if (typeof window === 'undefined' || !window.location) {
      return '';
    }
    return window.location.port || (window.location.protocol === 'https:' ? '443' : '80');
  }
}
