/**
 * A machine-to-machine credential. Note there is no `clientSecret` field: the plaintext secret
 * only ever exists in an ApiClientCredentials response, returned by create and rotate.
 */
export interface ApiClient {
  id: number;
  clientId: string;
  name: string;
  description?: string | null;
  enabled: boolean;
  expiresAt?: string | null;
  createdBy?: string | null;
  createdDate?: string | null;
  lastUsedDate?: string | null;
  expired: boolean;
}

/** Create/rotate response — the only time the secret is available. It cannot be fetched again. */
export interface ApiClientCredentials {
  client: ApiClient;
  clientId: string;
  clientSecret: string;
}

/** Payload for create and update. */
export interface ApiClientRequest {
  name: string;
  description?: string | null;
  expiresAt?: string | null;
}
