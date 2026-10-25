import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { PortalContextService } from '../../../core/services/portal-context.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
})
export class TopbarComponent {
  /** Emitted when the hamburger button is tapped (mobile only). */
  @Output() openSidebar = new EventEmitter<void>();

  constructor(
    public authService: AuthService,
    private portalContext: PortalContextService,
  ) {}

  isAdmin(): boolean {
    if (!this.portalContext.isAdminPortal()) return false;
    return this.authService.currentUser()?.authorities?.includes('ROLE_ADMIN') ?? false;
  }

  logout(): void {
    this.authService.logout();
  }

  get userDisplay(): string {
    const user = this.authService.currentUser();
    if (!user) return '';
    if (user.firstName) {
      return user.lastName ? `${user.firstName} ${user.lastName}` : user.firstName;
    }
    return user.login;
  }

  get userInitials(): string {
    const user = this.authService.currentUser();
    if (!user) return '?';
    if (user.firstName) {
      return (user.firstName.charAt(0) + (user.lastName?.charAt(0) ?? '')).toUpperCase();
    }
    return user.login.charAt(0).toUpperCase();
  }
}
