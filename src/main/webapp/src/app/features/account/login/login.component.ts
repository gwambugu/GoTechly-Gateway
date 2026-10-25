import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  username = '';
  password = '';
  rememberMe = false;
  loading = signal(false);
  error = signal('');
  returnUrl = '/home';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/home';

    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/home']);
    }
  }

  login(): void {
    if (!this.username || !this.password) {
      this.error.set('Please enter username and password');
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authService.login({
      username: this.username,
      password: this.password,
      rememberMe: this.rememberMe
    }).subscribe({
      next: () => {
        this.router.navigate([this.returnUrl]);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        // The server distinguishes a bad password (401) from an account that is pending approval,
        // locked or expired (403); show its message so the second group is not told to retype a
        // password that was never the problem.
        this.error.set(err?.error?.detail ?? 'Invalid username or password');
      }
    });
  }
}
