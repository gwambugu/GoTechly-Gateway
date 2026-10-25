import { Component, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { TopbarComponent } from './shared/components/topbar/topbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ToastContainerComponent } from './shared/components/toast-container/toast-container.component';

// Routes where the sidenav shell should NOT be rendered
const PUBLIC_PATHS = new Set(['/', '/login', '/register', '/reset-password', '/reset/finish']);

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, TopbarComponent, FooterComponent, ToastContainerComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'GoTechly';

  /** True when the sidenav + footer shell should be visible. */
  readonly showShell = signal(false);

  constructor(private router: Router) {
    // Sync with current browser URL on page load / hard refresh
    this.showShell.set(!PUBLIC_PATHS.has(window.location.pathname));

    // Keep in sync on every subsequent client-side navigation
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        const path = e.urlAfterRedirects.split('?')[0];
        this.showShell.set(!PUBLIC_PATHS.has(path));
      });
  }
}
