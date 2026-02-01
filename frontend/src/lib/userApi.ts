import { apiClient } from './api';
import type { ApiResponse, PagedResponse } from '../types/api';
import type {
	UserDto,
	UserCreateRequest,
	UserUpdateRequest,
	PasswordChangeRequest,
	UserListParams,
} from '../types/user';

class UserApi {
	async createUser(data: UserCreateRequest): Promise<ApiResponse<UserDto>> {
		return apiClient.post<UserDto>('/users', data);
	}

	async getUsers(params: UserListParams = {}): Promise<ApiResponse<PagedResponse<UserDto>>> {
		const queryParams = new URLSearchParams();
		if (params.page !== undefined) queryParams.set('page', params.page.toString());
		if (params.size !== undefined) queryParams.set('size', params.size.toString());
		if (params.status) queryParams.set('status', params.status);
		if (params.role) queryParams.set('role', params.role);
		if (params.orgId) queryParams.set('orgId', params.orgId);
		if (params.search) queryParams.set('search', params.search);

		const queryString = queryParams.toString();
		const endpoint = `/users${queryString ? `?${queryString}` : ''}`;

		return apiClient.get<PagedResponse<UserDto>>(endpoint);
	}

	async getUserById(id: string): Promise<ApiResponse<UserDto>> {
		return apiClient.get<UserDto>(`/users/${id}`);
	}

	async updateUser(id: string, data: UserUpdateRequest): Promise<ApiResponse<UserDto>> {
		return apiClient.put<UserDto>(`/users/${id}`, data);
	}

	async deleteUser(id: string): Promise<ApiResponse<void>> {
		return apiClient.delete<void>(`/users/${id}`);
	}

	async changePassword(id: string, data: PasswordChangeRequest): Promise<ApiResponse<void>> {
		return apiClient.post<void>(`/users/${id}/change-password`, data);
	}
}

export const userApi = new UserApi();
