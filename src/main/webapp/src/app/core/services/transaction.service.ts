import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { PaymentLogDTO, AuditLogDTO } from '../../shared/models/transaction.model';

export interface PaginatedResult<T> {
  items: T[];
  totalCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly CORE_API = '/services/core/api';

  constructor(private http: HttpClient) {}

  getPaymentLogs(
    page: number = 0,
    size: number = 20,
    filters?: Record<string, string>,
    source: 'active' | 'history' | 'pending' = 'active',
  ): Observable<PaginatedResult<PaymentLogDTO>> {
    const endpoint =
      source === 'history'
        ? 'history-payment-logs'
        : source === 'pending'
          ? 'pending-payment-logs'
          : 'payment-logs';
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'id,desc');

    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value) {
          params = params.set(key, value);
        }
      });
    }

    return this.http
      .get<PaymentLogDTO[]>(`${this.CORE_API}/${endpoint}`, { params, observe: 'response' })
      .pipe(
        map((response: HttpResponse<PaymentLogDTO[]>) => ({
          items: response.body ?? [],
          totalCount: +(response.headers.get('X-Total-Count') ?? '0'),
        })),
      );
  }

  getPaymentLog(
    id: number,
    source: 'active' | 'history' | 'pending' = 'active',
  ): Observable<PaymentLogDTO> {
    const endpoint =
      source === 'history'
        ? 'history-payment-logs'
        : source === 'pending'
          ? 'pending-payment-logs'
          : 'payment-logs';
    return this.http.get<PaymentLogDTO>(`${this.CORE_API}/${endpoint}/${id}`);
  }

  getAuditLogs(
    page: number = 0,
    size: number = 50,
    filters?: Record<string, string>,
  ): Observable<PaginatedResult<AuditLogDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      // createdAt carries the stage clock, not the insert clock — audit rows are written by an
      // async pool, so id order is persistence order and scrambles the execution sequence.
      // id is the tiebreaker: stages skipped by one halt all share a timestamp.
      .set('sort', 'createdAt,asc')
      .append('sort', 'id,asc');

    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value) {
          params = params.set(key, value);
        }
      });
    }

    return this.http
      .get<AuditLogDTO[]>(`${this.CORE_API}/audit-logs`, { params, observe: 'response' })
      .pipe(
        map((response: HttpResponse<AuditLogDTO[]>) => ({
          items: response.body ?? [],
          totalCount: +(response.headers.get('X-Total-Count') ?? '0'),
        })),
      );
  }

  getAuditLogsForTransaction(requestId: string): Observable<AuditLogDTO[]> {
    const params = new HttpParams()
      .set('requestId.equals', requestId)
      .set('sort', 'createdAt,asc')
      .append('sort', 'id,asc')
      .set('size', '100');
    return this.http.get<AuditLogDTO[]>(`${this.CORE_API}/audit-logs`, { params });
  }
}
