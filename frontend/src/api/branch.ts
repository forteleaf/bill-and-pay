import { apiClient } from './client';
import { buildEndpoint, toApiDateFormat } from './queryUtils';
import type { ApiResponse, PagedResponse, OrgTree } from '@/types/api';
import type {
  Branch,
  BranchCreateRequest,
  BranchUpdateRequest,
  BranchListParams
} from '@/types/branch';

class BranchApi {
  async getBranches(params: BranchListParams = {}): Promise<ApiResponse<PagedResponse<Branch>>> {
    return apiClient.get<PagedResponse<Branch>>(buildEndpoint('/organizations', {
      page: params.page,
      size: params.size,
      type: params.type,
      status: params.status,
      search: params.search,
      startDate: toApiDateFormat(params.startDate),
      endDate: toApiDateFormat(params.endDate),
    }));
  }

  async getBranchById(id: string): Promise<ApiResponse<Branch>> {
    return apiClient.get<Branch>(`/organizations/${id}`);
  }

  async createBranch(data: BranchCreateRequest): Promise<ApiResponse<Branch>> {
    return apiClient.post<Branch>('/organizations', data);
  }

  async updateBranch(id: string, data: BranchUpdateRequest): Promise<ApiResponse<Branch>> {
    return apiClient.put<Branch>(`/organizations/${id}`, data);
  }

  async getBranchTree(): Promise<ApiResponse<OrgTree[]>> {
    return apiClient.get<OrgTree[]>('/organizations/tree');
  }

  async getBranchChildren(parentId: string): Promise<ApiResponse<Branch[]>> {
    return apiClient.get<Branch[]>(`/organizations/${parentId}/children`);
  }
}

export const branchApi = new BranchApi();

// Re-export businessEntityApi for backward compatibility
export { businessEntityApi } from './businessEntity';
