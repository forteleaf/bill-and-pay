export enum PgConnectionStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  MAINTENANCE = 'MAINTENANCE'
}

export const PG_CONNECTION_STATUS_LABELS: Record<PgConnectionStatus, string> = {
  [PgConnectionStatus.ACTIVE]: '활성',
  [PgConnectionStatus.INACTIVE]: '비활성',
  [PgConnectionStatus.MAINTENANCE]: '점검중'
};

export interface PgConnectionDto {
  id: number;
  pgCode: string;
  pgName: string;
  merchantId: string;
  apiBaseUrl: string;
  webhookBaseUrl?: string;
  tenantId?: string;
  status: PgConnectionStatus;
  createdAt: string;
  updatedAt: string;
  webhookUrl?: string;
}

export interface PgConnectionCreateRequest {
  pgCode: string;
  pgName: string;
  apiBaseUrl: string;
  webhookBaseUrl?: string;
  merchantId: string;
  apiKey: string;
  secretKey: string;
  webhookSecret?: string;
  timeoutMs?: number;
  retryCount?: number;
}

export interface PgConnectionUpdateRequest {
  pgName?: string;
  apiBaseUrl?: string;
  webhookBaseUrl?: string;
  merchantId?: string;
  apiKey?: string;
  secretKey?: string;
  webhookSecret?: string;
  timeoutMs?: number;
  retryCount?: number;
  status?: PgConnectionStatus;
}
