import { apiClient } from './api';
import type { ApiResponse } from '../types/api';
import type {
	FeeConfigurationDto,
	FeeConfigurationCreateRequest,
	FeeConfigurationUpdateRequest,
	FeeConfigHistoryDto
} from '../types/feeConfiguration';

class FeeConfigApi {
	async listByMerchant(merchantId: string): Promise<ApiResponse<FeeConfigurationDto[]>> {
		return apiClient.get<FeeConfigurationDto[]>(`/fee-configurations/merchant/${merchantId}`);
	}

	async getById(id: string): Promise<ApiResponse<FeeConfigurationDto>> {
		return apiClient.get<FeeConfigurationDto>(`/fee-configurations/${id}`);
	}

	async create(
		merchantId: string,
		data: FeeConfigurationCreateRequest
	): Promise<ApiResponse<FeeConfigurationDto>> {
		return apiClient.post<FeeConfigurationDto>(`/fee-configurations/merchant/${merchantId}`, data);
	}

	async update(
		id: string,
		data: FeeConfigurationUpdateRequest
	): Promise<ApiResponse<FeeConfigurationDto>> {
		return apiClient.put<FeeConfigurationDto>(`/fee-configurations/${id}`, data);
	}

	async deactivate(id: string, reason?: string): Promise<ApiResponse<void>> {
		const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
		return apiClient.put<void>(`/fee-configurations/${id}/deactivate${query}`, {});
	}

	async activate(id: string, reason?: string): Promise<ApiResponse<void>> {
		const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
		return apiClient.put<void>(`/fee-configurations/${id}/activate${query}`, {});
	}

	async getHistory(feeConfigId: string): Promise<ApiResponse<FeeConfigHistoryDto[]>> {
		return apiClient.get<FeeConfigHistoryDto[]>(`/fee-configurations/${feeConfigId}/history`);
	}

	async getMerchantHistory(merchantId: string): Promise<ApiResponse<FeeConfigHistoryDto[]>> {
		return apiClient.get<FeeConfigHistoryDto[]>(
			`/fee-configurations/merchant/${merchantId}/history`
		);
	}
}

export const feeConfigApi = new FeeConfigApi();
