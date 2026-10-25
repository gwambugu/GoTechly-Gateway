import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface JposInstanceMetrics {
  instance: string;
  baseUrl: string;
  status?: string;
  error?: string;
  transactionManagers?: TransactionManagerMetrics[];
  summary?: JposSummary;
}

export interface TransactionManagerMetrics {
  name: string;
  sessions: number;
  maxSessions: number;
  activeSessions: number;
  idleSessions: number;
  outstandingTransactions: number;
  inTransit: number;
  tps: string;
  tpsAvg: number;
  tpsPeak: number;
}

export interface JposSummary {
  totalTMs: number;
  totalSessions: number;
  totalActiveSessions: number;
  totalIdleSessions: number;
  totalOutstanding: number;
  totalInTransit: number;
}

export interface ScaleSessionsRequest {
  instance?: string;
  tmName: string;
  sessions: number;
  maxSessions: number;
}

export interface ScaleTmCountRequest {
  instance?: string;
  prefix: string;
  count: number;
}

@Injectable({
  providedIn: 'root'
})
export class JposMetricsService {
  constructor(private http: HttpClient) {}

  getMetrics(): Observable<JposInstanceMetrics[]> {
    return this.http.get<JposInstanceMetrics[]>('/api/admin/jpos/metrics');
  }

  scaleSessions(request: ScaleSessionsRequest): Observable<any[]> {
    return this.http.post<any[]>('/api/admin/jpos/scale/sessions', request);
  }

  scaleTmCount(request: ScaleTmCountRequest): Observable<any[]> {
    return this.http.post<any[]>('/api/admin/jpos/scale/tm-count', request);
  }
}
