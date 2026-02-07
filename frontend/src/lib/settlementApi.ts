import { apiClient } from './api';
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
  MerchantSettlementBreakdown,
  MerchantStatement,
  DailyStatementRow,
  OrgDailySettlementSummary,
  OrgDailySettlementDetail,
  OrgSettlementBreakdown,
  OrgStatement,
  DailyOrgStatementRow
} from '../types/api';
import type {
  SettlementListParams,
  SettlementSummaryParams,
  SettlementBatchListParams,
  OrganizationSettlementParams,
  DailySettlementParams,
  MerchantStatementParams,
  OrgDailySettlementParams,
  OrgStatementParams
} from '../types/settlement';
import { toApiDateTime } from './utils';

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
    const queryParams = new URLSearchParams();

    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.entityType) queryParams.set('entityType', params.entityType);
    if (params.status) queryParams.set('status', params.status);
    if (params.sortBy) queryParams.set('sortBy', params.sortBy);
    if (params.direction) queryParams.set('direction', params.direction);

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<Settlement>>(endpoint);
  }

  async getSummary(params: SettlementSummaryParams = {}): Promise<ApiResponse<SettlementSummary>> {
    const queryParams = new URLSearchParams();

    if (params.entityType) queryParams.set('entityType', params.entityType);

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements/summary${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<SettlementSummary>(endpoint);
  }

  async getDailyBatchReport(date: string): Promise<ApiResponse<Settlement[]>> {
    const dateApi = date.replace(/\//g, '-');
    return apiClient.get<Settlement[]>(`/settlements/batch/${dateApi}`);
  }

  async getBatches(params: SettlementBatchListParams = {}): Promise<ApiResponse<PagedResponse<SettlementBatch>>> {
    const queryParams = new URLSearchParams();

    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.status) queryParams.set('status', params.status);

    const startDateApi = params.startDate?.replace(/\//g, '-');
    const endDateApi = params.endDate?.replace(/\//g, '-');

    if (startDateApi) queryParams.set('startDate', startDateApi);
    if (endDateApi) queryParams.set('endDate', endDateApi);

    const queryString = queryParams.toString();
    const endpoint = `/settlements/batches${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<SettlementBatch>>(endpoint);
  }

  async getSettlementById(id: string): Promise<ApiResponse<Settlement>> {
    return apiClient.get<Settlement>(`/settlements/${id}`);
  }

  async getSettlementsByOrganization(params: OrganizationSettlementParams = {}): Promise<ApiResponse<OrganizationSettlementSummary[]>> {
    const queryParams = new URLSearchParams();

    if (params.orgType) queryParams.set('orgType', params.orgType);
    if (params.search) queryParams.set('search', params.search);

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements/by-organization${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<OrganizationSettlementSummary[]>(endpoint);
  }

  async resettleByEventId(transactionEventId: string): Promise<ApiResponse<Settlement[]>> {
    return apiClient.post<Settlement[]>('/settlements/resettle', { transactionEventId });
  }

  async getOrganizationSettlementDetail(
    organizationId: string,
    startDate?: string,
    endDate?: string
  ): Promise<ApiResponse<OrganizationSettlementDetail>> {
    const queryParams = new URLSearchParams();

    const startDateTime = toApiDateTime(startDate, '00:00:00');
    const endDateTime = toApiDateTime(endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements/organization/${organizationId}/details${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<OrganizationSettlementDetail>(endpoint);
  }

  async getMerchantDailySummary(params: DailySettlementParams = {}): Promise<ApiResponse<DailySettlementSummary[]>> {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.set('startDate', params.startDate.replace(/\//g, '-'));
    if (params.endDate) queryParams.set('endDate', params.endDate.replace(/\//g, '-'));
    if (params.status) queryParams.set('status', params.status);
    const queryString = queryParams.toString();
    return apiClient.get<DailySettlementSummary[]>(`/settlements/merchant-daily${queryString ? `?${queryString}` : ''}`);
  }

  async getMerchantDailyDetail(date: string): Promise<ApiResponse<DailySettlementDetail>> {
    const dateApi = date.replace(/\//g, '-');
    return apiClient.get<DailySettlementDetail>(`/settlements/merchant-daily/${dateApi}`);
  }

  async getMerchantStatement(params: MerchantStatementParams): Promise<ApiResponse<MerchantStatement>> {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.set('startDate', params.startDate.replace(/\//g, '-'));
    if (params.endDate) queryParams.set('endDate', params.endDate.replace(/\//g, '-'));
    const queryString = queryParams.toString();
    return apiClient.get<MerchantStatement>(`/settlements/merchant-statement/${params.merchantId}${queryString ? `?${queryString}` : ''}`);
  }

  async getOrgDailySummary(params: OrgDailySettlementParams = {}): Promise<ApiResponse<OrgDailySettlementSummary[]>> {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.set('startDate', params.startDate.replace(/\//g, '-'));
    if (params.endDate) queryParams.set('endDate', params.endDate.replace(/\//g, '-'));
    if (params.status) queryParams.set('status', params.status);
    const queryString = queryParams.toString();
    return apiClient.get<OrgDailySettlementSummary[]>(`/settlements/org-daily${queryString ? `?${queryString}` : ''}`);
  }

  async getOrgDailyDetail(date: string): Promise<ApiResponse<OrgDailySettlementDetail>> {
    const dateApi = date.replace(/\//g, '-');
    return apiClient.get<OrgDailySettlementDetail>(`/settlements/org-daily/${dateApi}`);
  }

  async getOrgStatement(params: OrgStatementParams): Promise<ApiResponse<OrgStatement>> {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.set('startDate', params.startDate.replace(/\//g, '-'));
    if (params.endDate) queryParams.set('endDate', params.endDate.replace(/\//g, '-'));
    const queryString = queryParams.toString();
    return apiClient.get<OrgStatement>(`/settlements/org-statement/${params.orgId}${queryString ? `?${queryString}` : ''}`);
  }
}

export const settlementApi = new SettlementApi();
