import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiClient, ApiClientCredentials, ApiClientRequest } from '../../../shared/models/api-client.model';

@Injectable({
  providedIn: 'root'
})
export class ApiClientManagementService {
  private readonly API_URL = '/api/admin/api-clients';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiClient[]> {
    return this.http.get<ApiClient[]>(this.API_URL);
  }

  create(request: ApiClientRequest): Observable<ApiClientCredentials> {
    return this.http.post<ApiClientCredentials>(this.API_URL, request);
  }

  update(id: number, request: ApiClientRequest): Observable<ApiClient> {
    return this.http.put<ApiClient>(`${this.API_URL}/${id}`, request);
  }

  /** Returns a brand new secret; the previous one stops working immediately. */
  rotateSecret(id: number): Observable<ApiClientCredentials> {
    return this.http.post<ApiClientCredentials>(`${this.API_URL}/${id}/rotate-secret`, {});
  }

  revoke(id: number): Observable<ApiClient> {
    return this.http.post<ApiClient>(`${this.API_URL}/${id}/revoke`, {});
  }

  enable(id: number): Observable<ApiClient> {
    return this.http.post<ApiClient>(`${this.API_URL}/${id}/enable`, {});
  }
}
