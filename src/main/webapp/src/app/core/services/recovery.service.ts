import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DeclaredOutcome,
  DeferredRecovery,
  RecoveryMetrics,
  RecoveryState,
} from '../../shared/models/recovery.model';


/** A page of rows plus the server's total, read from `X-Total-Count`. */
export interface RecoveryPage {
  rows: DeferredRecovery[];
  total: number;
}

@Injectable({
  providedIn: 'root'
})
export class RecoveryService {
  private readonly CORE_API = '/services/core/api/v1/admin/recovery';

  constructor(private http: HttpClient) {}

  getMetrics(): Observable<RecoveryMetrics> {
    return this.http.get<RecoveryMetrics>(`${this.CORE_API}/metrics`);
  }

  /**
   * Server-side paging — unlike plugin-management, which filters a fully loaded list client-side.
   * This table is unbounded: it grows with every unconfirmed posting, so the page has to come from
   * the server and the count from the header.
   */
  list(state: RecoveryState | 'ALL', page: number, size: number): Observable<RecoveryPage> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (state !== 'ALL') {
      params = params.set('state', state);
    }
    return new Observable<RecoveryPage>(subscriber => {
      const sub = this.http
        .get<DeferredRecovery[]>(this.CORE_API, { params, observe: 'response' })
        .subscribe({
          next: (response: HttpResponse<DeferredRecovery[]>) => {
            subscriber.next({
              rows: response.body ?? [],
              total: Number(response.headers.get('X-Total-Count') ?? 0)
            });
            subscriber.complete();
          },
          error: err => subscriber.error(err)
        });
      return () => sub.unsubscribe();
    });
  }

  get(requestId: string): Observable<DeferredRecovery> {
    return this.http.get<DeferredRecovery>(`${this.CORE_API}/${encodeURIComponent(requestId)}`);
  }

  /** Puts an escalated or cancelled row back to work, with a fresh attempt budget. */
  requeue(requestId: string): Observable<DeferredRecovery> {
    return this.http.post<DeferredRecovery>(
      `${this.CORE_API}/${encodeURIComponent(requestId)}/requeue`, {});
  }

  /**
   * Supplies the missing answer for an UNKNOWN row. The only route out for one — requeuing alone
   * escalates it again, because requeuing supplies no new information.
   *
   * Returns 409 if the row already has an established outcome; that value came from the message the
   * corridor actually sent and must not be overwritten from here.
   */
  setDeclaredOutcome(requestId: string, outcome: DeclaredOutcome, reason: string): Observable<DeferredRecovery> {
    const params = new HttpParams().set('outcome', outcome).set('reason', reason);
    return this.http.post<DeferredRecovery>(
      `${this.CORE_API}/${encodeURIComponent(requestId)}/declared-outcome`, {}, { params });
  }

  /** Stops trying — for an inconsistency being settled outside the system. */
  cancel(requestId: string, reason: string): Observable<DeferredRecovery> {
    const params = new HttpParams().set('reason', reason);
    return this.http.post<DeferredRecovery>(
      `${this.CORE_API}/${encodeURIComponent(requestId)}/cancel`, {}, { params });
  }
}
