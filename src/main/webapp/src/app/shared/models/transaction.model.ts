export interface PaymentLogDTO {
  id: number;
  paymentType?: string;
  requestType?: string;
  requestId?: string;
  requestTimestamp?: string;
  amount?: number;
  currencyCode?: string;
  status?: string;
  responseCode?: string;
  responseDesc?: string;
  sourceSystem?: string;
  direction?: string;
  // Sender
  senderMsisdn?: string;
  senderAccountNumber?: string;
  senderSenderName?: string;
  senderType?: string;
  // Receiver
  receiverMsisdn?: string;
  receiverAccountNumber?: string;
  receiverReceiverName?: string;
  receiverType?: string;
  // CBS
  cbsTranRef?: string;
  cbsRespCode?: string;
  cbsRespDesc?: string;
  cbsDrAcc?: string;
  cbsCrAcc?: string;
  // PSP
  pspResponseCode?: string;
  pspResponseDesc?: string;
  pspResultCode?: string;
  pspReferenceCode?: string;
  pspRequestID?: string;
  // Additional fields
  senderAdditionalFields?: Record<string, string>;
  receiverAdditionalFields?: Record<string, string>;
  detailsAdditionalFields?: Record<string, string>;
  cbsAdditionalFields?: Record<string, string>;
  pspAdditionalFields?: Record<string, string>;
}

export interface AuditLogDTO {
  id: number;
  eventType?: string;
  entityName?: string;
  phaseName?: string;
  oldValue?: string;
  newValue?: string;
  changedFields?: string;
  actorId?: string;
  actorType?: string;
  actorUsername?: string;
  role?: string;
  ipAddress?: string;
  userAgent?: string;
  sessionId?: string;
  requestId?: string;
  endpoint?: string;
  httpMethod?: string;
  environment?: string;
  serviceName?: string;
  createdAt?: string;
}
