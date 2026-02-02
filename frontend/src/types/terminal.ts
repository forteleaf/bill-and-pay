export enum TerminalStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
  TERMINATED = 'TERMINATED'
}

export enum TerminalType {
  CAT = 'CAT',
  POS = 'POS',
  MOBILE = 'MOBILE',
  KIOSK = 'KIOSK',
  ONLINE = 'ONLINE'
}

export const TERMINAL_STATUS_LABELS: Record<TerminalStatus, string> = {
  [TerminalStatus.ACTIVE]: '정상',
  [TerminalStatus.INACTIVE]: '비활성',
  [TerminalStatus.SUSPENDED]: '정지',
  [TerminalStatus.TERMINATED]: '해지'
};

export const TERMINAL_TYPE_LABELS: Record<TerminalType, string> = {
  [TerminalType.CAT]: 'CAT 단말기',
  [TerminalType.POS]: 'POS 단말기',
  [TerminalType.MOBILE]: '모바일',
  [TerminalType.KIOSK]: '키오스크',
  [TerminalType.ONLINE]: '온라인'
};

export interface TerminalDto {
  id: string;
  tid: string;
  catId?: string;
  terminalType: TerminalType;
  merchantId: string;
  merchantName?: string;
  merchantCode?: string;
  organizationId?: string;
  organizationName?: string;
  serialNumber?: string;
  model?: string;
  manufacturer?: string;
  installAddress?: string;
  installDate?: string;
  status: TerminalStatus;
  lastTransactionAt?: string;
  config?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface TerminalListParams {
  page?: number;
  size?: number;
  status?: TerminalStatus;
  terminalType?: TerminalType;
  merchantId?: string;
  organizationId?: string;
  search?: string;
}

export interface TerminalCreateRequest {
  tid: string;
  catId?: string;
  terminalType: TerminalType;
  merchantId: string;
  serialNumber?: string;
  model?: string;
  manufacturer?: string;
  installAddress?: string;
  installDate?: string;
  config?: Record<string, unknown>;
}

export interface TerminalUpdateRequest {
  catId?: string;
  serialNumber?: string;
  model?: string;
  manufacturer?: string;
  installAddress?: string;
  status?: TerminalStatus;
  config?: Record<string, unknown>;
}
