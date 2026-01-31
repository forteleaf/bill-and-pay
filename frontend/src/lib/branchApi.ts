import { apiClient } from './api';
import type { ApiResponse, PagedResponse, OrgTree } from '../types/api';
import type { Branch, BranchCreateRequest, BranchUpdateRequest, BranchListParams } from '../types/branch';

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
    if (params.startDate) queryParams.set('startDate', params.startDate);
    if (params.endDate) queryParams.set('endDate', params.endDate);
    
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
