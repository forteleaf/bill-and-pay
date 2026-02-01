import { apiClient } from './api';
import type { ApiResponse, PagedResponse, OrgTree } from '../types/api';
import type { 
  Branch, 
  BranchCreateRequest, 
  BranchUpdateRequest, 
  BranchListParams,
  BusinessEntity,
  BusinessEntityCreateRequest
} from '../types/branch';

/**
 * Convert date from display format (yyyy/MM/dd) to API format (yyyy-MM-dd)
 */
function toApiDateFormat(date: string | undefined): string | undefined {
  if (!date) return undefined;
  return date.replace(/\//g, '-');
}

class BranchApi {
  /**
   * Get paginated list of branches (organizations)
   */
  async getBranches(params: BranchListParams = {}): Promise<ApiResponse<PagedResponse<Branch>>> {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.type) queryParams.set('type', params.type);
    if (params.status) queryParams.set('status', params.status);
    if (params.search) queryParams.set('search', params.search);
    const startDateApi = toApiDateFormat(params.startDate);
    const endDateApi = toApiDateFormat(params.endDate);
    if (startDateApi) queryParams.set('startDate', startDateApi);
    if (endDateApi) queryParams.set('endDate', endDateApi);
    
    const queryString = queryParams.toString();
    const endpoint = `/organizations${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<PagedResponse<Branch>>(endpoint);
  }

  /**
   * Get single branch by ID
   */
  async getBranchById(id: string): Promise<ApiResponse<Branch>> {
    return apiClient.get<Branch>(`/organizations/${id}`);
  }

  /**
   * Create new branch
   */
  async createBranch(data: BranchCreateRequest): Promise<ApiResponse<Branch>> {
    return apiClient.post<Branch>('/organizations', data);
  }

  /**
   * Update existing branch
   */
  async updateBranch(id: string, data: BranchUpdateRequest): Promise<ApiResponse<Branch>> {
    return apiClient.put<Branch>(`/organizations/${id}`, data);
  }

  /**
   * Get organization tree for hierarchy selection
   */
  async getBranchTree(): Promise<ApiResponse<OrgTree[]>> {
    return apiClient.get<OrgTree[]>('/organizations/tree');
  }

  /**
   * Get children of a specific organization
   */
  async getBranchChildren(parentId: string): Promise<ApiResponse<Branch[]>> {
    return apiClient.get<Branch[]>(`/organizations/${parentId}/children`);
  }
}

export const branchApi = new BranchApi();

class BusinessEntityApi {
  async getAll(params: { page?: number; size?: number } = {}): Promise<ApiResponse<PagedResponse<BusinessEntity>>> {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    
    const queryString = queryParams.toString();
    const endpoint = `/business-entities${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<PagedResponse<BusinessEntity>>(endpoint);
  }

  async getById(id: string): Promise<ApiResponse<BusinessEntity>> {
    return apiClient.get<BusinessEntity>(`/business-entities/${id}`);
  }

  async searchByBusinessNumber(businessNumber: string): Promise<ApiResponse<BusinessEntity | null>> {
    const cleanNumber = businessNumber.replace(/-/g, '');
    const formattedNumber = `${cleanNumber.slice(0, 3)}-${cleanNumber.slice(3, 5)}-${cleanNumber.slice(5)}`;
    return apiClient.get<BusinessEntity | null>(`/business-entities/search?businessNumber=${encodeURIComponent(formattedNumber)}`);
  }

  async searchByName(name: string): Promise<ApiResponse<BusinessEntity[]>> {
    return apiClient.get<BusinessEntity[]>(`/business-entities/search/name?name=${encodeURIComponent(name)}`);
  }

  async create(data: BusinessEntityCreateRequest): Promise<ApiResponse<BusinessEntity>> {
    return apiClient.post<BusinessEntity>('/business-entities', data);
  }

  async update(id: string, data: BusinessEntityCreateRequest): Promise<ApiResponse<BusinessEntity>> {
    return apiClient.put<BusinessEntity>(`/business-entities/${id}`, data);
  }
}

export const businessEntityApi = new BusinessEntityApi();
