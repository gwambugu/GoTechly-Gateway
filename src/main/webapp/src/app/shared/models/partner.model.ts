export type PartnerStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED';

export type PartnerType = 'BANK' | 'MTO' | 'AGGREGATOR' | 'MERCHANT';

export type KycStatus = 'VERIFIED' | 'PENDING' | 'EXPIRED';

export type AccountType = 'SETTLEMENT' | 'FEE' | 'SUSPENSE' | 'FLOAT';

export type Direction = 'INWARD' | 'OUTWARD';

export interface PartnerDTO {
  id?: number;
  partnerCode?: string;
  legalName?: string;
  clientShortName?: string;
  agentId?: string;
  partnerType?: PartnerType;
  status?: PartnerStatus;
  contactEmail?: string;
  contactPhone?: string;
  clientCertificate?: string;
  ipWhitelist?: string;
  settlementCycle?: string;
  contractStartDate?: string;
  contractEndDate?: string;
  kycStatus?: KycStatus;
  kycExpiryDate?: string;
  enabled?: boolean;
  makerId?: string;
  makerDate?: string;
  checkerId?: string;
  checkerDate?: string;
  checkerRemarks?: string;
}

export interface PartnerAccountDTO {
  id?: number;
  partnerId?: number;
  accountType?: AccountType;
  accountNumber?: string;
  accountName?: string;
  bankName?: string;
  bankCode?: string;
  branchCode?: string;
  currencyCode?: string;
  isPrimary?: boolean;
  status?: string;
}

export interface PartnerServiceDTO {
  id?: number;
  partnerId?: number;
  paymentType?: string;
  direction?: Direction;
  status?: string;
}

export interface PartnerTransactionLimitDTO {
  id?: number;
  partnerId?: number;
  paymentType?: string;
  direction?: Direction;
  currencyCode?: string;
  minAmount?: number;
  maxAmount?: number;
  dailyAmountLimit?: number;
  dailyCountLimit?: number;
  monthlyAmountLimit?: number;
  monthlyCountLimit?: number;
  feeAmount?: number;
  feePercentage?: number;
  status?: string;
}

export interface PartnerConfigDTO {
  id?: number;
  partnerId?: number;
  paramName?: string;
  paramValue?: string;
  paramDescription?: string;
}

export interface UniquenessCheck {
  clientShortName?: boolean;
  agentId?: boolean;
  contactEmail?: boolean;
  contactPhone?: boolean;
}

export const PARTNER_STATUS_LABELS: Record<PartnerStatus, string> = {
  PENDING_APPROVAL: 'Pending Approval',
  ACTIVE: 'Active',
  SUSPENDED: 'Suspended',
  REJECTED: 'Rejected'
};

export const PARTNER_TYPE_LABELS: Record<PartnerType, string> = {
  BANK: 'Bank',
  MTO: 'Money Transfer Operator',
  AGGREGATOR: 'Aggregator',
  MERCHANT: 'Merchant'
};

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  SETTLEMENT: 'Settlement',
  FEE: 'Fee',
  SUSPENSE: 'Suspense',
  FLOAT: 'Float'
};
