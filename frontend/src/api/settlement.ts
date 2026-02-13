import { apiClient } from './client';
import { buildEndpoint, toApiDateFormat, toApiDateTime } from './queryUtils';
import type {
  ApiResponse,
  PagedResponse,
  Settlement,
  SettlementSummary,
  SettlementBatch,
  OrganizationSettlementSummary,
  OrganizationSettlementDetail,
  DailySettlementSummary,
  DailySettlementDetail,
  MerchantStatement,
  OrgDailySettlementSummary,
  OrgDailySettlementDetail,
  OrgStatement,
} from '@/types/api';
import type {
  SettlementListParams,
  SettlementSummaryParams,
  SettlementBatchListParams,
  OrganizationSettlementParams,
  DailySettlementParams,
  MerchantStatementParams,
  OrgDailySettlementParams,
  OrgStatementParams
} from '@/types/settlement';

// Re-export types for convenience
export type {
  SettlementListParams,
  SettlementSummaryParams,
  SettlementBatchListParams,
  OrganizationSettlementParams,
  DailySettlementParams,
  MerchantStatementParams,
  OrgDailySettlementParams,
  OrgStatementParams
};

class SettlementApi {
  async getSettlements(params: SettlementListParams = {}): Promise<ApiResponse<PagedResponse<Settlement>>> {
    return apiClient.get<PagedResponse<Settlement>>(buildEndpoint('/settlements', {
      page: params.page,
      size: params.size,
      entityType: params.entityType,
      status: params.status,
      sortBy: params.sortBy,
      direction: params.direction,
      startDate: toApiDateTime(params.startDate, '00:00:00'),
      endDate: toApiDateTime(params.endDate, '23:59:59'),
    }));
  }

  async getSummary(params: SettlementSummaryParams = {}): Promise<ApiResponse<SettlementSummary>> {
    return apiClient.get<SettlementSummary>(buildEndpoint('/settlements/summary', {
      entityType: params.entityType,
      startDate: toApiDateTime(params.startDate, '00:00:00'),
      endDate: toApiDateTime(params.endDate, '23:59:59'),
    }));
  }

  async getDailyBatchReport(date: string): Promise<ApiResponse<Settlement[]>> {
    return apiClient.get<Settlement[]>(`/settlements/batch/${toApiDateFormat(date)}`);
  }

  async getBatches(params: SettlementBatchListParams = {}): Promise<ApiResponse<PagedResponse<SettlementBatch>>> {
    return apiClient.get<PagedResponse<SettlementBatch>>(buildEndpoint('/settlements/batches', {
      page: params.page,
      size: params.size,
      status: params.status,
      startDate: toApiDateFormat(params.startDate),
      endDate: toApiDateFormat(params.endDate),
    }));
  }

  async getSettlementById(id: string): Promise<ApiResponse<Settlement>> {
    return apiClient.get<Settlement>(`/settlements/${id}`);
  }

  async getSettlementsByOrganization(params: OrganizationSettlementParams = {}): Promise<ApiResponse<OrganizationSettlementSummary[]>> {
    return apiClient.get<OrganizationSettlementSummary[]>(buildEndpoint('/settlements/by-organization', {
      orgType: params.orgType,
      search: params.search,
      startDate: toApiDateTime(params.startDate, '00:00:00'),
      endDate: toApiDateTime(params.endDate, '23:59:59'),
    }));
  }

  async resettleByEventId(transactionEventId: string): Promise<ApiResponse<Settlement[]>> {
    return apiClient.post<Settlement[]>('/settlements/resettle', { transactionEventId });
  }

  async getOrganizationSettlementDetail(
    organizationId: string,
    startDate?: string,
    endDate?: string
  ): Promise<ApiResponse<OrganizationSettlementDetail>> {
    return apiClient.get<OrganizationSettlementDetail>(buildEndpoint(`/settlements/organization/${organizationId}/details`, {
      startDate: toApiDateTime(startDate, '00:00:00'),
      endDate: toApiDateTime(endDate, '23:59:59'),
    }));
  }

  async getMerchantDailySummary(params: DailySettlementParams = {}): Promise<ApiResponse<DailySettlementSummary[]>> {
    return apiClient.get<DailySettlementSummary[]>(buildEndpoint('/settlements/merchant-daily', {
      startDate: toApiDateFormat(params.startDate),
      endDate: toApiDateFormat(params.endDate),
      status: params.status,
    }));
  }

  async getMerchantDailyDetail(date: string): Promise<ApiResponse<DailySettlementDetail>> {
    return apiClient.get<DailySettlementDetail>(`/settlements/merchant-daily/${toApiDateFormat(date)}`);
  }

  async getMerchantStatement(params: MerchantStatementParams): Promise<ApiResponse<MerchantStatement>> {
    return apiClient.get<MerchantStatement>(buildEndpoint(`/settlements/merchant-statement/${params.merchantId}`, {
      startDate: toApiDateFormat(params.startDate),
      endDate: toApiDateFormat(params.endDate),
    }));
  }

  async getOrgDailySummary(params: OrgDailySettlementParams = {}): Promise<ApiResponse<OrgDailySettlementSummary[]>> {
    return apiClient.get<OrgDailySettlementSummary[]>(buildEndpoint('/settlements/org-daily', {
      startDate: toApiDateFormat(params.startDate),
      endDate: toApiDateFormat(params.endDate),
      status: params.status,
    }));
  }

  async getOrgDailyDetail(date: string): Promise<ApiResponse<OrgDailySettlementDetail>> {
    return apiClient.get<OrgDailySettlementDetail>(`/settlements/org-daily/${toApiDateFormat(date)}`);
  }

  async getOrgStatement(params: OrgStatementParams): Promise<ApiResponse<OrgStatement>> {
    return apiClient.get<OrgStatement>(buildEndpoint(`/settlements/org-statement/${params.orgId}`, {
      startDate: toApiDateFormat(params.startDate),
      endDate: toApiDateFormat(params.endDate),
    }));
  }
}

export const settlementApi = new SettlementApi();
