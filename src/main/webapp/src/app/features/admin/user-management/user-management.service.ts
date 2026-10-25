import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { AdminUserDTO } from '../../../shared/models/user.model';

export interface PaginatedResult<T> {
  items: T[];
  totalCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private readonly API_URL = '/api/admin/users';
  private readonly AUTHORITIES_URL = '/api/admin/authorities';

  constructor(private http: HttpClient) {}

  getAvailableAuthorities(): Observable<string[]> {
    return this.http.get<{ name: string }[]>(this.AUTHORITIES_URL).pipe(
      map(list => list
        .map(a => a.name)
        .filter(name => name !== 'ROLE_ANONYMOUS'))
    );
  }

  getPendingUsers(): Observable<AdminUserDTO[]> {
    return this.http.get<AdminUserDTO[]>(`${this.API_URL}/pending`);
  }

  approveUser(id: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/approve`, {});
  }

  rejectUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getAllUsers(page: number = 0, size: number = 20): Observable<PaginatedResult<AdminUserDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<AdminUserDTO[]>(this.API_URL, { params, observe: 'response' }).pipe(
      map((response: HttpResponse<AdminUserDTO[]>) => ({
        items: response.body ?? [],
        totalCount: +(response.headers.get('X-Total-Count') ?? '0')
      }))
    );
  }

  searchUsers(query: string): Observable<AdminUserDTO[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<AdminUserDTO[]>(`${this.API_URL}/search`, { params });
  }

  updateUserRole(id: number, authorities: string[]): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/roles`, authorities);
  }

  setUserEnabled(id: number, enabled: boolean): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/enabled`, enabled);
  }

  getOnlineUsers(): Observable<AdminUserDTO[]> {
    return this.http.get<AdminUserDTO[]>(`${this.API_URL}/online`);
  }
}
