export enum PgConnectionStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ERROR = 'ERROR'
}

export const PG_CONNECTION_STATUS_LABELS: Record<PgConnectionStatus, string> = {
  [PgConnectionStatus.ACTIVE]: '정상',
  [PgConnectionStatus.INACTIVE]: '비활성',
  [PgConnectionStatus.ERROR]: '오류'
};

export interface PgConnectionDto {
  id: string;
  pgCode: string;
  pgName: string;
  pgApiVersion?: string;
  merchantId: string;
  webhookPath: string;
  apiBaseUrl?: string;
  apiEndpoints?: Record<string, string>;
  status: PgConnectionStatus;
  lastSyncAt?: string;
  lastError?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PgConnectionCreateRequest {
  pgCode: string;
  pgName: string;
  pgApiVersion?: string;
  merchantId: string;
  apiKey: string;
  apiSecret: string;
  webhookPath: string;
  webhookSecret?: string;
  apiBaseUrl?: string;
  apiEndpoints?: Record<string, string>;
}

export interface PgConnectionUpdateRequest {
  pgName?: string;
  pgApiVersion?: string;
  apiKey?: string;
  apiSecret?: string;
  webhookSecret?: string;
  apiBaseUrl?: string;
  apiEndpoints?: Record<string, string>;
  status?: PgConnectionStatus;
}
