import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PluginInfo } from '../../shared/models/plugin.model';

@Injectable({
  providedIn: 'root'
})
export class PluginService {
  private readonly CORE_API = '/services/core/api/v1/admin';

  constructor(private http: HttpClient) {}

  getPlugins(): Observable<PluginInfo[]> {
    return this.http.get<PluginInfo[]>(`${this.CORE_API}/plugins`);
  }

  disablePlugin(paymentType: string): Observable<void> {
    return this.http.post<void>(`${this.CORE_API}/plugins/${paymentType}/disable`, {});
  }

  enablePlugin(paymentType: string): Observable<void> {
    return this.http.post<void>(`${this.CORE_API}/plugins/${paymentType}/enable`, {});
  }

  getDisabledPlugins(): Observable<string[]> {
    return this.http.get<string[]>(`${this.CORE_API}/plugins/disabled`);
  }
}
