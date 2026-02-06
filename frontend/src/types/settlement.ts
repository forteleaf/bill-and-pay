/**
 * Settlement API request parameter types
 */

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
