import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  AppConfigsDTO, ChannelsDTO, EndpointConfigsDTO, ErrorCodesDTO,
  AmountLimitsDTO, TemplateLookupDTO, BankCodesLookupDTO, PendingChangeDTO, EntityType
} from '../../shared/models/config.model';
import { PaginatedResult } from './transaction.service';

@Injectable({
  providedIn: 'root'
})
export class ConfigManagementService {
  private readonly CORE_API = '/services/core/api/v1/configs';

  constructor(private http: HttpClient) {}

  // ===== App Configs =====
  getAppConfigs(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<AppConfigsDTO>> {
    return this.getPaginated<AppConfigsDTO>(`${this.CORE_API}/app-configs`, page, size, filters);
  }

  getAppConfig(id: number): Observable<AppConfigsDTO> {
    return this.http.get<AppConfigsDTO>(`${this.CORE_API}/app-configs/${id}`);
  }

  // ===== Channels =====
  getChannels(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<ChannelsDTO>> {
    return this.getPaginated<ChannelsDTO>(`${this.CORE_API}/channels`, page, size, filters);
  }

  getChannel(id: number): Observable<ChannelsDTO> {
    return this.http.get<ChannelsDTO>(`${this.CORE_API}/channels/${id}`);
  }

  // ===== Endpoint Configs =====
  getEndpointConfigs(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<EndpointConfigsDTO>> {
    return this.getPaginated<EndpointConfigsDTO>(`${this.CORE_API}/endpoint-configs`, page, size, filters);
  }

  getEndpointConfig(id: number): Observable<EndpointConfigsDTO> {
    return this.http.get<EndpointConfigsDTO>(`${this.CORE_API}/endpoint-configs/${id}`);
  }

  // ===== Error Codes =====
  getErrorCodes(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<ErrorCodesDTO>> {
    return this.getPaginated<ErrorCodesDTO>(`${this.CORE_API}/error-codes`, page, size, filters);
  }

  getErrorCode(id: number): Observable<ErrorCodesDTO> {
    return this.http.get<ErrorCodesDTO>(`${this.CORE_API}/error-codes/${id}`);
  }

  // ===== Amount Limits =====
  getAmountLimits(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<AmountLimitsDTO>> {
    return this.getPaginated<AmountLimitsDTO>(`${this.CORE_API}/amount-limits`, page, size, filters);
  }

  getAmountLimit(id: number): Observable<AmountLimitsDTO> {
    return this.http.get<AmountLimitsDTO>(`${this.CORE_API}/amount-limits/${id}`);
  }

  // ===== Template Lookups =====
  getTemplateLookups(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<TemplateLookupDTO>> {
    return this.getPaginated<TemplateLookupDTO>(`${this.CORE_API}/template-lookups`, page, size, filters);
  }

  getTemplateLookup(id: number): Observable<TemplateLookupDTO> {
    return this.http.get<TemplateLookupDTO>(`${this.CORE_API}/template-lookups/${id}`);
  }

  // ===== Bank Codes Lookups =====
  getBankCodesLookups(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<BankCodesLookupDTO>> {
    return this.getPaginated<BankCodesLookupDTO>(`${this.CORE_API}/bank-codes-lookups`, page, size, filters);
  }

  getBankCodesLookup(id: number): Observable<BankCodesLookupDTO> {
    return this.http.get<BankCodesLookupDTO>(`${this.CORE_API}/bank-codes-lookups/${id}`);
  }

  // ===== Pending Changes =====
  getPendingChanges(page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<PendingChangeDTO>> {
    return this.getPaginated<PendingChangeDTO>(`${this.CORE_API}/pending-changes`, page, size, filters, 'id,desc');
  }

  getPendingChange(id: number): Observable<PendingChangeDTO> {
    return this.http.get<PendingChangeDTO>(`${this.CORE_API}/pending-changes/${id}`);
  }

  submitChange(dto: PendingChangeDTO): Observable<PendingChangeDTO> {
    return this.http.post<PendingChangeDTO>(`${this.CORE_API}/pending-changes`, dto);
  }

  approveChange(id: number): Observable<PendingChangeDTO> {
    return this.http.put<PendingChangeDTO>(`${this.CORE_API}/pending-changes/${id}/approve`, {});
  }

  rejectChange(id: number, remarks?: string): Observable<PendingChangeDTO> {
    return this.http.put<PendingChangeDTO>(`${this.CORE_API}/pending-changes/${id}/reject`, { remarks });
  }

  // ===== Generic entity fetch by type =====
  getEntities(entityType: EntityType, page = 0, size = 20, filters?: Record<string, string>): Observable<PaginatedResult<any>> {
    const path = this.getApiPath(entityType);
    return this.getPaginated(path, page, size, filters);
  }

  getEntity(entityType: EntityType, id: number): Observable<any> {
    const path = this.getApiPath(entityType);
    return this.http.get(`${path}/${id}`);
  }

  private getApiPath(entityType: EntityType): string {
    switch (entityType) {
      case 'APP_CONFIGS': return `${this.CORE_API}/app-configs`;
      case 'CHANNELS': return `${this.CORE_API}/channels`;
      case 'ENDPOINT_CONFIGS': return `${this.CORE_API}/endpoint-configs`;
      case 'ERROR_CODES': return `${this.CORE_API}/error-codes`;
      case 'AMOUNT_LIMITS': return `${this.CORE_API}/amount-limits`;
      case 'TEMPLATE_LOOKUP': return `${this.CORE_API}/template-lookups`;
      case 'BANK_CODES_LOOKUP': return `${this.CORE_API}/bank-codes-lookups`;
      case 'LINKED_WALLET_SUBSCRIPTION': return `${this.CORE_API}/linked-wallet-subscriptions`;
    }
  }

  private getPaginated<T>(url: string, page: number, size: number,
                          filters?: Record<string, string>, sort = 'id,desc'): Observable<PaginatedResult<T>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value) params = params.set(key, value);
      });
    }

    return this.http.get<T[]>(url, { params, observe: 'response' }).pipe(
      map((response: HttpResponse<T[]>) => ({
        items: response.body ?? [],
        totalCount: +(response.headers.get('X-Total-Count') ?? '0')
      }))
    );
  }
}
