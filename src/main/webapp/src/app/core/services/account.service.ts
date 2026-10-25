import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ManagedUserVM, KeyAndPasswordVM, AdminUserDTO } from '../../shared/models/user.model';

/** Pre-authentication view of the gateway's auth mode. */
export interface SecurityConfig {
  adEnabled: boolean;
  domain: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly API_URL = '/api';

  constructor(private http: HttpClient) {}

  getSecurityConfig(): Observable<SecurityConfig> {
    return this.http.get<SecurityConfig>(`${this.API_URL}/security-config`);
  }

  register(user: ManagedUserVM): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/register`, user);
  }

  checkLoginAvailable(login: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/register/check-login`, { params: { login } });
  }

  checkEmailAvailable(email: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.API_URL}/register/check-email`, { params: { email } });
  }

  activate(key: string): Observable<void> {
    return this.http.get<void>(`${this.API_URL}/activate`, { params: { key } });
  }

  getAccount(): Observable<AdminUserDTO> {
    return this.http.get<AdminUserDTO>(`${this.API_URL}/account`);
  }

  saveAccount(user: AdminUserDTO): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/account`, user);
  }

  requestPasswordReset(mail: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/account/reset-password/init`, mail, {
      headers: { 'Content-Type': 'text/plain' }
    });
  }

  finishPasswordReset(keyAndPassword: KeyAndPasswordVM): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/account/reset-password/finish`, keyAndPassword);
  }
}
