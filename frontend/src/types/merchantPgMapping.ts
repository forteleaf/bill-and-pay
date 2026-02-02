export enum MerchantPgMappingStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export const MERCHANT_PG_MAPPING_STATUS_LABELS: Record<MerchantPgMappingStatus, string> = {
  [MerchantPgMappingStatus.ACTIVE]: '활성',
  [MerchantPgMappingStatus.INACTIVE]: '비활성'
};

export interface MerchantPgMappingDto {
  id: string;
  merchantId: string;
  merchantName?: string;
  merchantCode?: string;
  pgConnectionId: string;
  pgCode?: string;
  pgName?: string;
  mid: string;
  terminalId?: string;
  catId?: string;
  config?: Record<string, unknown>;
  status: MerchantPgMappingStatus;
  createdAt: string;
  updatedAt: string;
}

export interface MerchantPgMappingCreateRequest {
  merchantId: string;
  pgConnectionId: string;
  mid: string;
  terminalId?: string;
  catId?: string;
  config?: Record<string, unknown>;
  status?: MerchantPgMappingStatus;
}

export interface MerchantPgMappingUpdateRequest {
  pgConnectionId?: string;
  mid?: string;
  terminalId?: string;
  catId?: string;
  config?: Record<string, unknown>;
  status?: MerchantPgMappingStatus;
}
