import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteMetrics, MetricsSummary } from '../../shared/models/metrics.model';

@Injectable({
  providedIn: 'root'
})
export class MetricsService {
  constructor(private http: HttpClient) {}

  getGatewayMetrics(): Observable<RouteMetrics[]> {
    return this.http.get<RouteMetrics[]>('/api/admin/metrics/gateway');
  }

  getGatewaySummary(): Observable<MetricsSummary> {
    return this.http.get<MetricsSummary>('/api/admin/metrics/gateway/summary');
  }
}
