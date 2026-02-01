import { apiClient } from './api';
import type { ApiResponse, PagedResponse } from '../types/api';
import type {
	SettlementAccountDto,
	SettlementAccountCreateRequest,
	SettlementAccountUpdateRequest,
	SettlementAccountListParams,
	SettlementAccountVerifyRequest,
	SettlementAccountPrimaryResponse
} from '../types/settlementAccount';

class SettlementAccountApi {
	/**
	 * Create a new settlement account
	 */
	async create(data: SettlementAccountCreateRequest): Promise<ApiResponse<SettlementAccountDto>> {
		return apiClient.post<SettlementAccountDto>('/settlement-accounts', data);
	}

	/**
	 * Get settlement account by ID
	 */
	async getById(id: string): Promise<ApiResponse<SettlementAccountDto>> {
		return apiClient.get<SettlementAccountDto>(`/settlement-accounts/${id}`);
	}

	/**
	 * Update settlement account
	 */
	async update(
		id: string,
		data: SettlementAccountUpdateRequest
	): Promise<ApiResponse<SettlementAccountDto>> {
		return apiClient.put<SettlementAccountDto>(`/settlement-accounts/${id}`, data);
	}

	/**
	 * Soft delete settlement account
	 */
	async delete(id: string): Promise<ApiResponse<void>> {
		return apiClient.delete<void>(`/settlement-accounts/${id}`);
	}

	/**
	 * List settlement accounts by entity
	 */
	async listByEntity(
		entityType: string,
		entityId: string,
		params: SettlementAccountListParams = {}
	): Promise<ApiResponse<PagedResponse<SettlementAccountDto>>> {
		const queryParams = new URLSearchParams();
		if (params.page !== undefined) queryParams.set('page', params.page.toString());
		if (params.size !== undefined) queryParams.set('size', params.size.toString());
		if (params.status) queryParams.set('status', params.status);

		const queryString = queryParams.toString();
		const endpoint = `/settlement-accounts/entity/${entityType}/${entityId}${queryString ? `?${queryString}` : ''}`;

		return apiClient.get<PagedResponse<SettlementAccountDto>>(endpoint);
	}

	/**
	 * Get primary settlement account for entity
	 */
	async getPrimary(
		entityType: string,
		entityId: string
	): Promise<ApiResponse<SettlementAccountPrimaryResponse>> {
		return apiClient.get<SettlementAccountPrimaryResponse>(
			`/settlement-accounts/entity/${entityType}/${entityId}/primary`
		);
	}

	/**
	 * Set settlement account as primary
	 */
	async setPrimary(id: string): Promise<ApiResponse<SettlementAccountDto>> {
		return apiClient.put<SettlementAccountDto>(`/settlement-accounts/${id}/set-primary`, {});
	}

	/**
	 * Verify settlement account
	 */
	async verify(
		id: string,
		data?: SettlementAccountVerifyRequest
	): Promise<ApiResponse<SettlementAccountDto>> {
		return apiClient.put<SettlementAccountDto>(
			`/settlement-accounts/${id}/verify`,
			data || {}
		);
	}
}

export const settlementAccountApi = new SettlementAccountApi();
