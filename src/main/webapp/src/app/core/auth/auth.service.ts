import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { LoginVM, JWTToken, AdminUserDTO } from '../../shared/models/user.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly API_URL = '/api';
  private readonly TOKEN_KEY = 'authenticationToken';
  private readonly USER_KEY = 'user';

  // Signal so anything reading it from a template re-renders automatically when
  // the user is hydrated (after login or post-bootstrap loadUser()). Initial
  // value is whatever was persisted in localStorage so the menu paints
  // correctly on a fresh page load before /api/account returns.
  private readonly currentUserSignal = signal<AdminUserDTO | null>(this.readStoredUser());
  public readonly currentUser = this.currentUserSignal.asReadonly();

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {
    queueMicrotask(() => this.loadStoredToken());
  }

  private loadStoredToken(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (token) {
      this.loadUser();
    }
  }

  login(credentials: LoginVM): Observable<JWTToken> {
    return this.http.post<JWTToken>(`${this.API_URL}/authenticate`, credentials).pipe(
      tap((response) => {
        this.storeToken(response.id_token);
        this.loadUser();
      }),
    );
  }
  private storeToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private loadUser(): void {
    this.http
      .get<AdminUserDTO>(`${this.API_URL}/account`)
      .pipe(
        catchError((err) => {
          console.error('Failed to load user account:', err.status, err.message);
          return of(null);
        }),
      )
      .subscribe((user) => {
        if (user) {
          this.currentUserSignal.set(user);
          localStorage.setItem(this.USER_KEY, JSON.stringify(user));
        }
      });
  }

  getCurrentUser(): AdminUserDTO | null {
    return this.currentUserSignal();
  }

  private readStoredUser(): AdminUserDTO | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }
  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }
}
