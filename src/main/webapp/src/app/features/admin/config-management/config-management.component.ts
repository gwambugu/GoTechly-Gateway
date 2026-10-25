import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigManagementService } from '../../../core/services/config-management.service';
import { ToastService } from '../../../core/services/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { EntityType, ENTITY_LABELS, PendingChangeDTO } from '../../../shared/models/config.model';

interface SearchField {
  /** Backend filter key including operator, e.g. "errorCode.contains". */
  filterKey: string;
  placeholder: string;
}

interface TabConfig {
  entityType: EntityType;
  label: string;
  columns: { key: string; label: string; mono?: boolean }[];
  /** One input per entry; results match when ALL non-empty fields match (AND). */
  searchFields: SearchField[];
  /** When true, the grid is view-only: New Record + Edit buttons are hidden. */
  readOnly?: boolean;
}

@Component({
  selector: 'app-config-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './config-management.component.html',
  styleUrl: './config-management.component.scss'
})
export class ConfigManagementComponent implements OnInit {

  tabs: TabConfig[] = [
    {
      entityType: 'APP_CONFIGS', label: 'App Configs',
      columns: [
        { key: 'configId', label: 'Config ID', mono: true },
        { key: 'configValue', label: 'Value' },
        { key: 'serviceType', label: 'Service Type' },
        { key: 'configStatus', label: 'Status' },
        { key: 'isSensitive', label: 'Sensitive' }
      ],
      searchFields: [{ filterKey: 'configId.contains', placeholder: 'Search by Config ID...' }]
    },
    {
      entityType: 'CHANNELS', label: 'Channels',
      columns: [
        { key: 'channelName', label: 'Channel Name' },
        { key: 'systemName', label: 'System Name' },
        { key: 'apiName', label: 'API Name' },
        { key: 'authType', label: 'Auth Type' },
        { key: 'contentType', label: 'Content Type' },
        { key: 'sourceIps', label: 'Source IPs' },
        { key: 'status', label: 'Status' }
      ],
      searchFields: [{ filterKey: 'channelName.contains', placeholder: 'Search by Channel Name...' }]
    },
    {
      entityType: 'ENDPOINT_CONFIGS', label: 'Endpoints',
      columns: [
        { key: 'serviceName', label: 'Service Name', mono: true },
        { key: 'endpoint', label: 'Endpoint' },
        { key: 'serviceType', label: 'Type' },
        { key: 'authType', label: 'Auth' },
        { key: 'providerCode', label: 'Provider' }
      ],
      searchFields: [{ filterKey: 'serviceName.contains', placeholder: 'Search by Service Name...' }]
    },
    {
      entityType: 'ERROR_CODES', label: 'Error Codes',
      columns: [
        { key: 'apiName', label: 'API Name', mono: true },
        { key: 'errorCode', label: 'Error Code', mono: true },
        { key: 'errorDescription', label: 'Description' },
        { key: 'serviceName', label: 'Service' },
        { key: 'serviceType', label: 'Service Type' },
        { key: 'transactionState', label: 'State' },
        { key: 'requestType', label: 'Request Type' }
      ],
      searchFields: [
        { filterKey: 'apiName.contains', placeholder: 'API Name' },
        { filterKey: 'serviceName.contains', placeholder: 'Service Name' },
        { filterKey: 'serviceType.contains', placeholder: 'Service Type' },
        { filterKey: 'requestType.contains', placeholder: 'Request Type' },
        { filterKey: 'errorCode.contains', placeholder: 'Error Code' }
      ]
    },
    {
      entityType: 'AMOUNT_LIMITS', label: 'Amount Limits',
      columns: [
        { key: 'channelName', label: 'Channel Name' },
        { key: 'serviceName', label: 'Service Name' },
        { key: 'countryCode', label: 'Country', mono: true },
        { key: 'currencyCode', label: 'Currency', mono: true },
        { key: 'transactionLimit', label: 'Transaction Limit' },
        { key: 'dailyLimit', label: 'Daily Limit' },
        { key: 'status', label: 'Status' }
      ],
      searchFields: [
        { filterKey: 'channelName.contains',  placeholder: 'Channel Name' },
        { filterKey: 'serviceName.contains',  placeholder: 'Service Name' },
        { filterKey: 'countryCode.contains',  placeholder: 'Country Code' },
        { filterKey: 'currencyCode.contains', placeholder: 'Currency Code' }
      ]
    },
    {
      // Template Lookup is intentionally rendered without the `hm` column.
      // The backing DB column exists but is never bound to UI inputs nor
      // displayed in the grid (see TemplateLookupDTO TS model).
      entityType: 'TEMPLATE_LOOKUP', label: 'Template Lookup',
      columns: [
        { key: 'templateName', label: 'Template Name', mono: true },
        { key: 'providerCode', label: 'Provider' },
        { key: 'url', label: 'URL' },
        { key: 'contentType', label: 'Content Type' },
        { key: 'modifiedBy', label: 'Modified By' },
        { key: 'modifyDate', label: 'Modified Date' }
      ],
      searchFields: [
        { filterKey: 'templateName.contains', placeholder: 'Template Name' },
        { filterKey: 'providerCode.contains', placeholder: 'Provider Code' },
        { filterKey: 'url.contains',          placeholder: 'URL' }
      ]
    },
    {
      entityType: 'BANK_CODES_LOOKUP', label: 'Bank Codes Lookup',
      columns: [
        { key: 'countryCode', label: 'Country', mono: true },
        { key: 'bankCode', label: 'Bank Code', mono: true },
        { key: 'bankName', label: 'Bank Name' },
        { key: 'bankShortName', label: 'Short Name' },
        { key: 'bankBranchCode', label: 'Branch', mono: true },
        { key: 'bic', label: 'BIC', mono: true },
        { key: 'pic', label: 'PIC' },
        { key: 'bin', label: 'BIN' },
        { key: 'status', label: 'Status' }
      ],
      searchFields: [
        { filterKey: 'countryCode.contains',   placeholder: 'Country Code' },
        { filterKey: 'bankCode.contains',      placeholder: 'Bank Code' },
        { filterKey: 'bankName.contains',      placeholder: 'Bank Name' },
        { filterKey: 'bankShortName.contains', placeholder: 'Short Name' },
        { filterKey: 'bic.contains',           placeholder: 'BIC' },
        { filterKey: 'status.contains',        placeholder: 'Status' }
      ]
    },
    {
      // Read-only for now: the Core resource exposes only GET endpoints. Rows are written
      // by the plugin flows, not the admin UI — M-KESHO payment subscriptions and, since
      // the account/alias (proxy) linkage feature, rows written by LinkedAccountService
      // (serviceType = 'ACCOUNT_LINKAGE'). The grid therefore mixes both shapes: linkage
      // rows leave vmtRef/pspName empty, M-KESHO rows leave paymentType/aliasName empty.
      // Search fields mirror the indexed columns (savings_accountnumber,
      // mobile_phonenumber, vmtref, alias_name, request_id, payment_type).
      entityType: 'LINKED_WALLET_SUBSCRIPTION', label: 'Linked Wallet Subscription',
      readOnly: true,
      columns: [
        { key: 'serviceType', label: 'Service Type' },
        { key: 'paymentType', label: 'Scheme', mono: true },
        { key: 'pspName', label: 'PSP' },
        { key: 'mobilePhoneNumber', label: 'Mobile', mono: true },
        { key: 'savingsAccountNumber', label: 'Savings Acct', mono: true },
        { key: 'aliasName', label: 'Alias / Proxy', mono: true },
        { key: 'bankShortCode', label: 'Bank', mono: true },
        { key: 'vmtRef', label: 'VMT Ref', mono: true },
        { key: 'linkStatus', label: 'Link Status' },
        { key: 'dateLinked', label: 'Date Linked' },
        { key: 'status', label: 'Status' }
      ],
      // requestId is searchable but not a column: it is the id a channel quotes when
      // chasing a registration, and a 70-char correlation id in the grid is noise.
      searchFields: [
        { filterKey: 'savingsAccountNumber.contains', placeholder: 'Savings Account' },
        { filterKey: 'mobilePhoneNumber.contains',    placeholder: 'Mobile Number' },
        { filterKey: 'aliasName.contains',            placeholder: 'Alias / Proxy' },
        { filterKey: 'requestId.contains',            placeholder: 'Request ID' },
        { filterKey: 'paymentType.contains',          placeholder: 'Scheme' },
        { filterKey: 'vmtRef.contains',               placeholder: 'VMT Ref' },
        { filterKey: 'pspName.contains',              placeholder: 'PSP Name' }
      ]
    }
  ];

  activeTab: EntityType = 'APP_CONFIGS';
  items: any[] = [];
  totalCount = 0;
  currentPage = 0;
  pageSize = 20;
  loading = true;
  /** Per-field search input keyed by SearchField.filterKey (e.g. "errorCode.contains"). */
  searchQueries: Record<string, string> = {};

  // Form state
  showForm = false;
  formMode: 'create' | 'edit' = 'create';
  formData: any = {};
  formEntityType: EntityType = 'APP_CONFIGS';
  formSubmitted = false;
  duplicateError = '';
  submitting = false;
  jsonErrors: Record<string, string> = {};

  constructor(
    private configService: ConfigManagementService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadItems();
  }

  get activeTabConfig(): TabConfig {
    return this.tabs.find(t => t.entityType === this.activeTab)!;
  }

  switchTab(entityType: EntityType): void {
    this.activeTab = entityType;
    this.searchQueries = {};
    this.currentPage = 0;
    this.showForm = false;
    this.loadItems();
  }

  loadItems(): void {
    this.loading = true;
    const filters: Record<string, string> = {};
    for (const f of this.activeTabConfig.searchFields) {
      const v = this.searchQueries[f.filterKey];
      if (v && v.trim()) filters[f.filterKey] = v.trim();
    }

    this.configService.getEntities(this.activeTab, this.currentPage, this.pageSize, filters).subscribe({
      next: (result) => {
        this.items = result.items;
        this.totalCount = result.totalCount;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load records');
        this.cdr.detectChanges();
      }
    });
  }

  applySearch(): void {
    this.currentPage = 0;
    this.loadItems();
  }

  clearSearch(): void {
    this.searchQueries = {};
    this.currentPage = 0;
    this.loadItems();
  }

  /** True when at least one search input has a non-blank value. */
  hasActiveSearch(): boolean {
    return this.activeTabConfig.searchFields.some(f => {
      const v = this.searchQueries[f.filterKey];
      return v != null && v.trim() !== '';
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadItems();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadItems();
  }

  openCreateForm(): void {
    this.formMode = 'create';
    this.formEntityType = this.activeTab;
    this.formData = {};
    this.formSubmitted = false;
    this.duplicateError = '';
    this.submitting = false;
    this.jsonErrors = {};
    this.showForm = true;
  }

  openEditForm(item: any): void {
    this.formMode = 'edit';
    this.formEntityType = this.activeTab;
    this.formData = { ...item };
    // Convert JSON-typed fields from object to a pretty-printed string for textarea editing.
    this.getFormFields().filter(f => f.type === 'json').forEach(f => {
      const v = this.formData[f.key];
      if (v != null && typeof v === 'object') {
        this.formData[f.key] = JSON.stringify(v, null, 2);
      }
    });
    this.formSubmitted = false;
    this.duplicateError = '';
    this.submitting = false;
    this.jsonErrors = {};
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.formData = {};
    this.formSubmitted = false;
    this.duplicateError = '';
    this.submitting = false;
    this.jsonErrors = {};
  }

  /** True when a required field is empty or whitespace-only. */
  isBlank(key: string): boolean {
    const v = this.formData[key];
    return v == null || (typeof v === 'string' && v.trim() === '');
  }

  submitForm(description: string): void {
    this.formSubmitted = true;
    this.duplicateError = '';
    this.jsonErrors = {};

    // Trim string inputs in place so the backend stores the canonical form
    // and downstream uniqueness checks compare apples to apples.
    Object.keys(this.formData).forEach(k => {
      const v = this.formData[k];
      if (typeof v === 'string') this.formData[k] = v.trim();
    });
    const trimmedDesc = (description ?? '').trim();

    // Required-field check: rejects blank AND whitespace-only.
    const requiredFields = this.getFormFields().filter(f => f.required);
    const missingFields = requiredFields.filter(f => this.isBlank(f.key));
    if (missingFields.length > 0 || !trimmedDesc) {
      return;
    }

    // Parse JSON fields back to objects for the outgoing payload. Keep the raw
    // string in formData so the textarea still rebinds correctly if the
    // backend rejects the submission.
    const payload: any = { ...this.formData };
    const jsonFields = this.getFormFields().filter(f => f.type === 'json');
    for (const f of jsonFields) {
      const raw = this.formData[f.key];
      if (raw == null || raw === '') {
        payload[f.key] = null;
        continue;
      }
      try {
        const parsed = JSON.parse(raw);
        if (typeof parsed !== 'object' || Array.isArray(parsed)) {
          this.jsonErrors[f.key] = `${f.label} must be a JSON object`;
        } else {
          payload[f.key] = parsed;
        }
      } catch {
        this.jsonErrors[f.key] = `${f.label} is not valid JSON`;
      }
    }
    if (Object.keys(this.jsonErrors).length > 0) {
      return;
    }

    // For app_configs CREATE, query the backend for an exact configId match
    // before submitting — gives the maker an immediate error instead of waiting
    // for the checker to discover the conflict at approval time.
    if (this.formEntityType === 'APP_CONFIGS' && this.formMode === 'create') {
      this.submitting = true;
      this.configService.getEntities('APP_CONFIGS', 0, 1, {
        'configId.equals': this.formData.configId
      }).subscribe({
        next: (res) => {
          if (res.items.length > 0) {
            this.submitting = false;
            this.duplicateError = `Config ID '${this.formData.configId}' already exists`;
            this.cdr.detectChanges();
            return;
          }
          this.commitChange(trimmedDesc, payload);
        },
        error: () => {
          this.submitting = false;
          this.toast.error('Could not verify Config ID uniqueness — please try again');
          this.cdr.detectChanges();
        }
      });
    } else {
      this.commitChange(trimmedDesc, payload);
    }
  }

  private commitChange(description: string, payload: any): void {
    const dto: PendingChangeDTO = {
      entityType: this.formEntityType,
      changeType: this.formMode === 'create' ? 'CREATE' : 'UPDATE',
      entityId: this.formMode === 'edit' ? this.formData.id : undefined,
      payload: JSON.stringify(payload),
      description: description
    };

    this.submitting = true;
    this.configService.submitChange(dto).subscribe({
      next: () => {
        this.submitting = false;
        this.toast.success('Change submitted for approval');
        this.closeForm();
      },
      error: (err) => {
        this.submitting = false;
        // Backend can also reject on configIdBlank / configIdDuplicate at
        // approval time even if our pre-check passed (race window).
        const errorKey = err.error?.errorKey;
        if (errorKey === 'configIdDuplicate') {
          this.duplicateError = err.error?.detail ?? err.error?.title ?? 'Config ID already exists';
        } else if (errorKey === 'configIdBlank') {
          this.duplicateError = 'Config ID is required';
        } else {
          this.toast.error('Failed to submit change: ' + (err.error?.message || err.message));
        }
        this.cdr.detectChanges();
      }
    });
  }

  getFieldValue(item: any, key: string): string {
    const val = item[key];
    if (val == null) return '-';
    if (typeof val === 'object') return JSON.stringify(val);
    const str = String(val);
    return str.length > 60 ? str.substring(0, 60) + '...' : str;
  }

  /** Returns the full value when it was truncated, so it can be shown as a hover tooltip. */
  getTooltip(item: any, key: string): string {
    const val = item[key];
    if (val == null || typeof val === 'object') return '';
    const str = String(val);
    return str.length > 60 ? str : '';
  }

  getFormFields(): { key: string; label: string; type: string; required?: boolean }[] {
    switch (this.formEntityType) {
      case 'APP_CONFIGS':
        return [
          { key: 'configId', label: 'Config ID', type: 'text', required: true },
          { key: 'configValue', label: 'Config Value', type: 'textarea', required: true },
          { key: 'serviceType', label: 'Service Type', type: 'text' },
          { key: 'configStatus', label: 'Config Status', type: 'text' },
          { key: 'isSensitive', label: 'Sensitive (encrypt value, mask on display)', type: 'boolean' }
        ];
      case 'CHANNELS':
        return [
          { key: 'channelName', label: 'Channel Name', type: 'text', required: true },
          { key: 'systemName', label: 'System Name', type: 'text', required: true },
          { key: 'apiName', label: 'API Name', type: 'text', required: true },
          { key: 'callbackUrl', label: 'Callback URL', type: 'text' },
          { key: 'callbackRequestTemplate', label: 'Callback Template', type: 'textarea' },
          { key: 'contentType', label: 'Content Type', type: 'text', required: true },
          { key: 'authType', label: 'Auth Type', type: 'text' },
          { key: 'authCertificate', label: 'Auth Certificate (PEM)', type: 'textarea' },
          { key: 'expectedResponse', label: 'Expected Response', type: 'text' },
          { key: 'additionalFields', label: 'Additional Fields (JSON)', type: 'json' },
          { key: 'sourceIps', label: 'Source IPs', type: 'text' },
          { key: 'status', label: 'Status', type: 'text' }
        ];
      case 'ENDPOINT_CONFIGS':
        return [
          { key: 'serviceName', label: 'Service Name', type: 'text', required: true },
          { key: 'endpoint', label: 'Endpoint URL', type: 'text', required: true },
          { key: 'serviceType', label: 'Service Type', type: 'text' },
          { key: 'requestType', label: 'Request Type', type: 'text' },
          { key: 'contentType', label: 'Content Type', type: 'text' },
          { key: 'authType', label: 'Auth Type', type: 'text' },
          { key: 'providerCode', label: 'Provider Code', type: 'text' },
          { key: 'timeout', label: 'Timeout (ms)', type: 'number' },
          { key: 'reqTemplate', label: 'Request Template', type: 'textarea' },
          { key: 'statusExp', label: 'Status Expression', type: 'text' },
          { key: 'tranIDExp', label: 'Transaction ID Expression', type: 'text' },
          { key: 'errorExp', label: 'Error Expression', type: 'text' },
          { key: 'additionalHeader1', label: 'Additional Header 1', type: 'text' },
          { key: 'additionalHeader2', label: 'Additional Header 2', type: 'text' },
          { key: 'additionalHeader3', label: 'Additional Header 3', type: 'text' },
          { key: 'param1', label: 'Param 1', type: 'text' },
          { key: 'param2', label: 'Param 2', type: 'text' },
          { key: 'param3', label: 'Param 3', type: 'text' }
        ];
      case 'ERROR_CODES':
        return [
          { key: 'apiName', label: 'API Name', type: 'text' },
          { key: 'serviceName', label: 'Service Name', type: 'text', required: true },
          { key: 'serviceType', label: 'Service Type', type: 'text' },
          { key: 'requestType', label: 'Request Type', type: 'text' },
          { key: 'errorCode', label: 'Error Code', type: 'text', required: true },
          { key: 'errorDescription', label: 'Error Description', type: 'text', required: true },
          { key: 'transactionState', label: 'Transaction State', type: 'text' }
        ];
      case 'AMOUNT_LIMITS':
        return [
          { key: 'channelName', label: 'Channel Name', type: 'text', required: true },
          { key: 'serviceName', label: 'Service Name', type: 'text', required: true },
          { key: 'countryCode', label: 'Country Code (ISO)', type: 'text', required: true },
          { key: 'currencyCode', label: 'Currency Code (ISO 4217)', type: 'text', required: true },
          { key: 'transactionLimit', label: 'Transaction Limit', type: 'number', required: true },
          { key: 'dailyLimit', label: 'Daily Limit', type: 'number', required: true },
          { key: 'status', label: 'Status', type: 'text' }
        ];
      case 'TEMPLATE_LOOKUP':
        // Deliberately omits `hm` (security-sensitive, never editable from UI).
        // `reqTemplate` is a BLOB on Core but surfaced as text (UTF-8 round-trip).
        return [
          { key: 'templateName', label: 'Template Name', type: 'text', required: true },
          { key: 'providerCode', label: 'Provider Code', type: 'text' },
          { key: 'url', label: 'URL', type: 'text' },
          { key: 'contentType', label: 'Content Type', type: 'text' },
          { key: 'reqTemplate', label: 'Request Template', type: 'textarea' },
          { key: 'additionalHeader1', label: 'Additional Header 1', type: 'text' },
          { key: 'additionalHeader2', label: 'Additional Header 2', type: 'text' },
          { key: 'statusExp', label: 'Status Expression', type: 'text' },
          { key: 'tranIdExp', label: 'Transaction ID Expression', type: 'text' },
          { key: 'errorExp', label: 'Error Expression', type: 'text' },
          { key: 'timeOutExp', label: 'Timeout Expression', type: 'text' },
          { key: 'param1', label: 'Param 1', type: 'text' },
          { key: 'param2', label: 'Param 2', type: 'text' },
          { key: 'additionalFields', label: 'Additional Fields (JSON)', type: 'json' }
        ];
      case 'BANK_CODES_LOOKUP':
        return [
          { key: 'countryCode', label: 'Country Code (ISO)', type: 'text', required: true },
          { key: 'bankCode', label: 'Bank Code', type: 'text', required: true },
          { key: 'bankName', label: 'Bank Name', type: 'text' },
          { key: 'bankShortName', label: 'Bank Short Name', type: 'text' },
          { key: 'bankBranchCode', label: 'Bank Branch Code', type: 'text' },
          { key: 'bic', label: 'BIC', type: 'text' },
          { key: 'pic', label: 'PIC', type: 'text' },
          { key: 'bin', label: 'BIN', type: 'text' },
          { key: 'additionalFields', label: 'Additional Fields (JSON)', type: 'json' },
          { key: 'status', label: 'Status', type: 'text' }
        ];
      case 'LINKED_WALLET_SUBSCRIPTION':
        // Read-only in the UI today (the tab hides New/Edit). Field set kept in
        // sync with the DTO so enabling writes later needs no extra wiring.
        // `hash1`/`hash2` integrity fields are intentionally never editable here.
        //
        // `requestId` and `dateLinked` are omitted for the same reason: both are
        // written by the account-linkage flow, not by an operator. requestId is
        // UNIQUE and is what the status API and the scheme's asynchronous callback
        // correlate on, so hand-editing it silently orphans a live registration;
        // dateLinked records when the scheme confirmed the linkage. Both are visible
        // in the grid and over the API — just not editable.
        return [
          { key: 'serviceType', label: 'Service Type', type: 'text' },
          { key: 'paymentType', label: 'Scheme (Payment Type)', type: 'text' },
          { key: 'aliasName', label: 'Alias / Proxy', type: 'text' },
          { key: 'pspName', label: 'PSP Name', type: 'text' },
          { key: 'mobilePhoneNumber', label: 'Mobile Phone Number', type: 'text' },
          { key: 'savingsAccountNumber', label: 'Savings Account Number', type: 'text' },
          { key: 'operatingCurrencyCode', label: 'Operating Currency Code', type: 'text' },
          { key: 'firstName', label: 'First Name', type: 'text' },
          { key: 'middleName', label: 'Middle Name', type: 'text' },
          { key: 'lastName', label: 'Last Name', type: 'text' },
          { key: 'dateOfBirth', label: 'Date of Birth', type: 'date' },
          { key: 'nationalId', label: 'National ID', type: 'text' },
          { key: 'passportNumber', label: 'Passport Number', type: 'text' },
          { key: 'countryCode', label: 'Country Code', type: 'text' },
          { key: 'cityTown', label: 'City / Town', type: 'text' },
          { key: 'address1', label: 'Address 1', type: 'text' },
          { key: 'address2', label: 'Address 2', type: 'text' },
          { key: 'postalCode', label: 'Postal Code', type: 'text' },
          { key: 'postalAddress', label: 'Postal Address', type: 'text' },
          { key: 'openingDate', label: 'Opening Date', type: 'date' },
          { key: 'agentNumber', label: 'Agent Number', type: 'text' },
          { key: 'agentName', label: 'Agent Name', type: 'text' },
          { key: 'bankName', label: 'Bank Name', type: 'text' },
          { key: 'bankShortCode', label: 'Bank Short Code', type: 'text' },
          { key: 'referenceNumber', label: 'Reference Number', type: 'text' },
          { key: 'actionCode', label: 'Action Code', type: 'text' },
          { key: 'status', label: 'Status', type: 'number' },
          { key: 'remarks', label: 'Remarks', type: 'text' },
          { key: 'pspDob', label: 'PSP DOB', type: 'text' },
          { key: 'pspNationalId', label: 'PSP National ID', type: 'text' },
          { key: 'pspPhone', label: 'PSP Phone', type: 'text' },
          { key: 'linkStatus', label: 'Link Status', type: 'text' },
          { key: 'linkTranId', label: 'Link Tran ID', type: 'text' },
          { key: 'vmtRefId', label: 'VMT Ref ID', type: 'number' },
          { key: 'vmtRef', label: 'VMT Ref', type: 'text' },
          { key: 'creditQualified', label: 'Credit Qualified', type: 'text' },
          { key: 'loanAccount', label: 'Loan Account', type: 'text' },
          { key: 'loanBalance', label: 'Loan Balance', type: 'number' },
          { key: 'additionalFields', label: 'Additional Fields (JSON)', type: 'json' }
        ];
    }
  }
}
