import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ServiceHealth } from '../../shared/models/service.model';

@Injectable({
  providedIn: 'root'
})
export class ServiceHealthService {
  constructor(private http: HttpClient) {}

  getServiceHealth(): Observable<ServiceHealth[]> {
    return this.http.get<ServiceHealth[]>('/api/admin/services');
  }

  /**
   * Take an instance out of the load-balancer pool. It finishes what it is already running.
   *
   * The Gateway refuses with 409 / LAST_ROUTABLE_NODE if this is the only instance still taking
   * traffic; `force` is the deliberate full-stop override.
   */
  drain(name: string, force = false): Observable<unknown> {
    const query = force ? '?force=true' : '';
    return this.http.post(`/api/admin/nodes/${encodeURIComponent(name)}/drain${query}`, {});
  }

  /** Put the instance back into the pool. */
  undrain(name: string): Observable<unknown> {
    return this.http.post(`/api/admin/nodes/${encodeURIComponent(name)}/undrain`, {});
  }
}
