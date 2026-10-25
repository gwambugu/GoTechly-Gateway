export interface AppConfigsDTO {
  id?: number;
  configId?: string;
  configValue?: string;
  serviceType?: string;
  configStatus?: string;
  /**
   * When true, the value is a secret. The backend returns it masked ("********")
   * on every read and stores it encrypted; on edit, leave the value blank to keep
   * the existing secret unchanged.
   */
  isSensitive?: boolean;
}

export interface ChannelsDTO {
  id?: number;
  channelName?: string;
  systemName?: string;
  apiName?: string;
  callbackUrl?: string;
  callbackRequestTemplate?: string;
  authCertificate?: string;
  contentType?: string;
  expectedResponse?: string;
  authType?: string;
  additionalFields?: Record<string, string>;
  sourceIps?: string;
  status?: string;
  dateCreated?: string;
  createdBy?: string;
  dateModified?: string;
  modifiedBy?: string;
}

export interface EndpointConfigsDTO {
  id?: number;
  serviceName?: string;
  serviceType?: string;
  requestType?: string;
  reqTemplate?: string;
  endpoint?: string;
  contentType?: string;
  additionalHeader1?: string;
  additionalHeader2?: string;
  additionalHeader3?: string;
  statusExp?: string;
  tranIDExp?: string;
  errorExp?: string;
  timeout?: number;
  timeOutExp?: string;
  providerCode?: string;
  param1?: string;
  param2?: string;
  param3?: string;
  authType?: string;
  modifiedBy?: string;
  modifyDate?: string;
  hm?: string;
}

export interface ErrorCodesDTO {
  id?: number;
  apiName?: string;
  serviceName?: string;
  serviceType?: string;
  requestType?: string;
  errorCode?: string;
  errorDescription?: string;
  transactionState?: string;
}

export interface AmountLimitsDTO {
  id?: number;
  channelName?: string;
  serviceName?: string;
  countryCode?: string;
  currencyCode?: string;
  transactionLimit?: number;
  dailyLimit?: number;
  status?: string;
  dateCreated?: string;
  createdBy?: string;
  dateModified?: string;
  modifiedBy?: string;
}

/**
 * Template Lookup row, surfaced through the admin maker-checker flow.
 *
 * The backing table has an `hm` column (varchar 200) which is intentionally
 * NOT exposed on this interface — admin UI users must not view or edit it.
 * Core preserves the existing value on UPDATE so approvals don't blank it.
 * `reqTemplate` is a BLOB on the backend, surfaced as text (Core converts
 * to/from UTF-8) since templates are text bodies (XML/JSON).
 * `additionalFields` is a free-form JSON object (CLOB on Core, Map<String,String>
 * in Java) — same treatment as Channels.additionalFields.
 */
export interface TemplateLookupDTO {
  id?: number;
  templateName?: string;
  contentType?: string;
  reqTemplate?: string;
  url?: string;
  additionalHeader1?: string;
  additionalHeader2?: string;
  statusExp?: string;
  tranIdExp?: string;
  errorExp?: string;
  timeOutExp?: string;
  providerCode?: string;
  param1?: string;
  param2?: string;
  additionalFields?: Record<string, string>;
  modifiedBy?: string;
  modifyDate?: string;
}

/**
 * Bank Codes Lookup row. `additionalFields` is a free-form JSON object on
 * Core (CLOB column, Map<String,String> in Java). The config-management form
 * binds it via a JSON textarea — same treatment as Channels.additionalFields.
 */
export interface BankCodesLookupDTO {
  id?: number;
  countryCode?: string;
  bankCode?: string;
  bankName?: string;
  bankShortName?: string;
  bankBranchCode?: string;
  bic?: string;
  pic?: string;
  bin?: string;
  additionalFields?: Record<string, string>;
  status?: string;
}

/**
 * Linked Wallet Subscription (M-KESHO) customer-registry row. Read-only in the
 * admin SPA for now — the Core resource exposes only GET endpoints; any write
 * would flow through the maker-checker `pending-changes` path (entityType
 * LINKED_WALLET_SUBSCRIPTION). `additionalFields` is a free-form JSON object on
 * Core (CLOB, Map<String,String>) — same treatment as Channels.additionalFields.
 * `hash1`/`hash2` are integrity fields: read here for completeness but never
 * surfaced in the grid or a form input.
 */
export interface LinkedWalletSubscriptionDTO {
  id?: number;
  /** Scheme that owns an account/alias linkage (e.g. RNDPS). Empty on M-KESHO rows. */
  paymentType?: string;
  /** The alias / proxy the customer claimed in the scheme's directory. */
  aliasName?: string;
  /** Channel correlation id for the linkage request — the key the status API reads by. */
  requestId?: string;
  /** ISO instant the linkage came into force; null while the scheme is still deciding. */
  dateLinked?: string;
  serviceType?: string;
  pspName?: string;
  mobilePhoneNumber?: string;
  savingsAccountNumber?: string;
  operatingCurrencyCode?: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  dateOfBirth?: string;
  nationalId?: string;
  passportNumber?: string;
  countryCode?: string;
  cityTown?: string;
  address1?: string;
  address2?: string;
  postalCode?: string;
  postalAddress?: string;
  openingDate?: string;
  agentNumber?: string;
  agentName?: string;
  bankName?: string;
  bankShortCode?: string;
  referenceNumber?: string;
  actionCode?: string;
  status?: number;
  remarks?: string;
  pspDob?: string;
  pspNationalId?: string;
  pspPhone?: string;
  createDate?: string;
  linkStatus?: string;
  linkTranId?: string;
  vmtRefId?: number;
  vmtRef?: string;
  creditQualified?: string;
  loanAccount?: string;
  loanBalance?: number;
  hash1?: string;
  hash2?: string;
  additionalFields?: Record<string, string>;
}

export interface PendingChangeDTO {
  id?: number;
  entityType?: string;
  changeType?: string;
  entityId?: number;
  payload?: string;
  previousValue?: string;
  status?: string;
  description?: string;
  makerUsername?: string;
  makerDate?: string;
  checkerUsername?: string;
  checkerDate?: string;
  checkerRemarks?: string;
}

export type EntityType =
  | 'APP_CONFIGS'
  | 'CHANNELS'
  | 'ENDPOINT_CONFIGS'
  | 'ERROR_CODES'
  | 'AMOUNT_LIMITS'
  | 'TEMPLATE_LOOKUP'
  | 'BANK_CODES_LOOKUP'
  | 'LINKED_WALLET_SUBSCRIPTION';

export const ENTITY_LABELS: Record<EntityType, string> = {
  APP_CONFIGS: 'App Configs',
  CHANNELS: 'Channels',
  ENDPOINT_CONFIGS: 'Endpoint Configs',
  ERROR_CODES: 'Error Codes',
  AMOUNT_LIMITS: 'Amount Limits',
  TEMPLATE_LOOKUP: 'Template Lookup',
  BANK_CODES_LOOKUP: 'Bank Codes Lookup',
  LINKED_WALLET_SUBSCRIPTION: 'Linked Wallet Subscription'
};
