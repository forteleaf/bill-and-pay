/**
 * Settlement API request parameter types
 */

export const SETTLEMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: '대기',
  COMPLETED: '완료',
  FAILED: '실패',
  CANCELLED: '취소',
};

export const TRANSACTION_STATUS_LABELS: Record<string, string> = {
  APPROVED: '승인',
  CANCELLED: '취소',
  PARTIAL_CANCELLED: '부분취소',
  PENDING: '대기',
  FAILED: '실패',
};

export interface SettlementListParams {
  page?: number;
  size?: number;
  entityType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
}

export interface SettlementSummaryParams {
  entityType?: string;
  startDate?: string;
  endDate?: string;
}

export interface SettlementBatchListParams {
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
}

export interface OrganizationSettlementParams {
  orgType?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
}

export interface DailySettlementParams {
  startDate?: string;
  endDate?: string;
  status?: string;
}

export interface MerchantStatementParams {
  merchantId: string;
  startDate?: string;
  endDate?: string;
}

export interface OrgDailySettlementParams {
  startDate?: string;
  endDate?: string;
  status?: string;
}

export interface OrgStatementParams {
  orgId: string;
  startDate?: string;
  endDate?: string;
}
