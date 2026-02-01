import { apiClient } from './api';
import type { ApiResponse } from '../types/api';
import type { ContactDto, ContactRole } from '../types/merchant';

export interface ContactCreateRequest {
	name: string;
	phone?: string;
	email?: string;
	role: ContactRole;
	entityType: string;
	entityId: string;
}

export interface ContactUpdateRequest {
	name?: string;
	phone?: string;
	email?: string;
	role?: ContactRole;
}

class ContactApi {
	async create(data: ContactCreateRequest): Promise<ApiResponse<ContactDto>> {
		return apiClient.post<ContactDto>('/contacts', data);
	}

	async getById(id: string): Promise<ApiResponse<ContactDto>> {
		return apiClient.get<ContactDto>(`/contacts/${id}`);
	}

	async update(id: string, data: ContactUpdateRequest): Promise<ApiResponse<ContactDto>> {
		return apiClient.put<ContactDto>(`/contacts/${id}`, data);
	}

	async delete(id: string): Promise<ApiResponse<void>> {
		return apiClient.delete<void>(`/contacts/${id}`);
	}

	async listByEntity(
		entityType: string,
		entityId: string
	): Promise<ApiResponse<ContactDto[]>> {
		return apiClient.get<ContactDto[]>(
			`/contacts/entity/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`
		);
	}

	async getPrimaryContact(
		entityType: string,
		entityId: string
	): Promise<ApiResponse<ContactDto>> {
		return apiClient.get<ContactDto>(
			`/contacts/entity/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}/primary`
		);
	}

	async setPrimary(id: string): Promise<ApiResponse<ContactDto>> {
		return apiClient.put<ContactDto>(`/contacts/${id}/set-primary`, {});
	}
}

export const contactApi = new ContactApi();
