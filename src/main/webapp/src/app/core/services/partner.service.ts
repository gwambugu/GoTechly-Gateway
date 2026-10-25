import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  PartnerDTO,
  PartnerAccountDTO,
  PartnerServiceDTO,
  PartnerTransactionLimitDTO,
  PartnerConfigDTO,
  UniquenessCheck
} from '../../shared/models/partner.model';

export interface PaginatedResult<T> {
  items: T[];
  totalCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class PartnerService {
  private readonly API = '/services/core/api/v1/partner';

  constructor(private http: HttpClient) {}

  // --- Partners ---

  getPartners(page: number = 0, size: number = 20, filters?: Record<string, string>): Observable<PaginatedResult<PartnerDTO>> {
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

    return this.http.get<PartnerDTO[]>(`${this.API}/partners`, { params, observe: 'response' }).pipe(
      map((response: HttpResponse<PartnerDTO[]>) => ({
        items: response.body ?? [],
        totalCount: +(response.headers.get('X-Total-Count') ?? '0')
      }))
    );
  }

  getPartner(id: number): Observable<PartnerDTO> {
    return this.http.get<PartnerDTO>(`${this.API}/partners/${id}`);
  }

  createPartner(partner: PartnerDTO): Observable<PartnerDTO> {
    return this.http.post<PartnerDTO>(`${this.API}/partners`, partner);
  }

  updatePartner(id: number, partner: PartnerDTO): Observable<PartnerDTO> {
    return this.http.put<PartnerDTO>(`${this.API}/partners/${id}`, partner);
  }

  deletePartner(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/partners/${id}`);
  }

  approvePartner(id: number, checkerId: string, remarks?: string): Observable<PartnerDTO> {
    return this.http.post<PartnerDTO>(`${this.API}/partners/${id}/approve`, { checkerId, remarks: remarks ?? '' });
  }

  rejectPartner(id: number, checkerId: string, remarks: string): Observable<PartnerDTO> {
    return this.http.post<PartnerDTO>(`${this.API}/partners/${id}/reject`, { checkerId, remarks });
  }

  suspendPartner(id: number, userId: string, remarks?: string): Observable<PartnerDTO> {
    return this.http.post<PartnerDTO>(`${this.API}/partners/${id}/suspend`, { userId, remarks: remarks ?? '' });
  }

  activatePartner(id: number, userId: string, remarks?: string): Observable<PartnerDTO> {
    return this.http.post<PartnerDTO>(`${this.API}/partners/${id}/activate`, { userId, remarks: remarks ?? '' });
  }

  checkUniqueness(params: Record<string, string>): Observable<UniquenessCheck> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value) {
        httpParams = httpParams.set(key, value);
      }
    });
    return this.http.get<UniquenessCheck>(`${this.API}/partners/check-unique`, { params: httpParams });
  }

  // --- Partner Accounts ---

  getAccounts(partnerId: number): Observable<PartnerAccountDTO[]> {
    return this.http.get<PartnerAccountDTO[]>(`${this.API}/partners/${partnerId}/accounts`);
  }

  createAccount(partnerId: number, account: PartnerAccountDTO): Observable<PartnerAccountDTO> {
    return this.http.post<PartnerAccountDTO>(`${this.API}/accounts`, account);
  }

  updateAccount(partnerId: number, accountId: number, account: PartnerAccountDTO): Observable<PartnerAccountDTO> {
    return this.http.put<PartnerAccountDTO>(`${this.API}/accounts/${accountId}`, account);
  }

  deleteAccount(partnerId: number, accountId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/accounts/${accountId}`);
  }

  checkAccountUniqueness(accountNumber: string): Observable<boolean> {
    return this.http.get<Record<string, boolean>>(`${this.API}/accounts/check-unique`, {
      params: new HttpParams().set('accountNumber', accountNumber)
    }).pipe(
      map(response => response['accountNumber'] ?? false)
    );
  }

  // --- Partner Services ---

  getServices(partnerId: number): Observable<PartnerServiceDTO[]> {
    return this.http.get<PartnerServiceDTO[]>(`${this.API}/partners/${partnerId}/services`);
  }

  createService(partnerId: number, service: PartnerServiceDTO): Observable<PartnerServiceDTO> {
    return this.http.post<PartnerServiceDTO>(`${this.API}/services`, service);
  }

  updateService(partnerId: number, serviceId: number, service: PartnerServiceDTO): Observable<PartnerServiceDTO> {
    return this.http.put<PartnerServiceDTO>(`${this.API}/services/${serviceId}`, service);
  }

  deleteService(partnerId: number, serviceId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/services/${serviceId}`);
  }

  // --- Transaction Limits ---

  getLimits(partnerId: number): Observable<PartnerTransactionLimitDTO[]> {
    return this.http.get<PartnerTransactionLimitDTO[]>(`${this.API}/partners/${partnerId}/limits`);
  }

  createLimit(partnerId: number, limit: PartnerTransactionLimitDTO): Observable<PartnerTransactionLimitDTO> {
    return this.http.post<PartnerTransactionLimitDTO>(`${this.API}/limits`, limit);
  }

  updateLimit(partnerId: number, limitId: number, limit: PartnerTransactionLimitDTO): Observable<PartnerTransactionLimitDTO> {
    return this.http.put<PartnerTransactionLimitDTO>(`${this.API}/limits/${limitId}`, limit);
  }

  deleteLimit(partnerId: number, limitId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/limits/${limitId}`);
  }

  // --- Partner Configs ---

  getConfigs(partnerId: number): Observable<PartnerConfigDTO[]> {
    return this.http.get<PartnerConfigDTO[]>(`${this.API}/partners/${partnerId}/configs`);
  }

  createConfig(partnerId: number, config: PartnerConfigDTO): Observable<PartnerConfigDTO> {
    return this.http.post<PartnerConfigDTO>(`${this.API}/configs`, config);
  }

  updateConfig(partnerId: number, configId: number, config: PartnerConfigDTO): Observable<PartnerConfigDTO> {
    return this.http.put<PartnerConfigDTO>(`${this.API}/configs/${configId}`, config);
  }

  deleteConfig(partnerId: number, configId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/configs/${configId}`);
  }
}
