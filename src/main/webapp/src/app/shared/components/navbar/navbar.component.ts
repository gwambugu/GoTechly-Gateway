import { Component, HostListener, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { PortalContextService } from '../../../core/services/portal-context.service';

const DESKTOP_BREAKPOINT = 1024;

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  isCollapsed = signal(false);

  /** True when the mobile drawer is slid in over the content. */
  isMobileOpen = signal(false);

  constructor(
    public authService: AuthService,
    private portalContext: PortalContextService,
    private router: Router,
  ) {
    // Auto-close the mobile drawer on every navigation so users don't have
    // to manually dismiss it after tapping a link.
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => this.isMobileOpen.set(false));
  }

  /** Close mobile drawer when the viewport grows past the desktop breakpoint. */
  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth >= DESKTOP_BREAKPOINT) {
      this.isMobileOpen.set(false);
    }
  }

  /** Called by the mobile topbar hamburger button. */
  openMobile(): void {
    this.isMobileOpen.set(true);
  }

  closeMobile(): void {
    this.isMobileOpen.set(false);
  }

  toggleCollapse(): void {
    // Collapse/expand only has meaning on desktop where the sidenav is visible.
    if (window.innerWidth < DESKTOP_BREAKPOINT) return;
    this.isCollapsed.update((v) => !v);
    document.documentElement.style.setProperty(
      '--sidenav-width',
      this.isCollapsed() ? '60px' : '240px',
    );
  }

  logout(): void {
    this.authService.logout();
  }

  /**
   * Admin links are only shown when the user has ROLE_ADMIN AND is accessing
   * the SPA from the admin portal port. Admin APIs called from the partner
   * portal port are rejected by the Gateway's PortAccessGlobalFilter (403),
   * so we hide the links there to avoid dead nav.
   */
  isAdmin(): boolean {
    if (!this.portalContext.isAdminPortal()) {
      return false;
    }
    return this.authService.currentUser()?.authorities?.includes('ROLE_ADMIN') ?? false;
  }

  hasPartnerAccess(): boolean {
    return (
      this.authService
        .currentUser()
        ?.authorities?.some(
          (r) =>
            r === 'ROLE_PARTNER_MAKER' ||
            r === 'ROLE_PARTNER_CHECKER' ||
            r === 'ROLE_PARTNER_VIEWER' ||
            r === 'ROLE_ADMIN',
        ) ?? false
    );
  }

  userInitials = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return '?';
    if (user.firstName) {
      const last = user.lastName?.charAt(0) ?? '';
      return (user.firstName.charAt(0) + last).toUpperCase();
    }
    return user.login.charAt(0).toUpperCase();
  });

  userDisplayName = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return '';
    if (user.firstName) {
      return user.lastName ? `${user.firstName} ${user.lastName}` : user.firstName;
    }
    return user.login;
  });

  userRoleLabel = computed(() => {
    const authorities = this.authService.currentUser()?.authorities ?? [];
    if (authorities.includes('ROLE_ADMIN')) return 'Administrator';
    if (authorities.includes('ROLE_PARTNER_MAKER')) return 'Partner Maker';
    if (authorities.includes('ROLE_PARTNER_CHECKER')) return 'Partner Checker';
    if (authorities.includes('ROLE_PARTNER_VIEWER')) return 'Partner Viewer';
    return 'User';
  });
}
