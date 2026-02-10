import { apiClient } from './client';
import type { ApiResponse, Transaction, PagedResponse } from '@/types/api';
import { toApiDateTime } from '$lib/utils';

export interface TransactionListParams {
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
  merchantId?: string;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
}

class TransactionApi {
  /**
   * Get transaction by ID
   */
  async getById(id: string): Promise<ApiResponse<Transaction>> {
    return apiClient.get<Transaction>(`/transactions/${id}`);
  }

  /**
   * Get paginated list of transactions
   */
  async getTransactions(params: TransactionListParams = {}): Promise<ApiResponse<PagedResponse<Transaction>>> {
    const queryParams = new URLSearchParams();

    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.status) queryParams.set('status', params.status);
    if (params.merchantId) queryParams.set('merchantId', params.merchantId);
    if (params.sortBy) queryParams.set('sortBy', params.sortBy);
    if (params.direction) queryParams.set('direction', params.direction);

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/transactions${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<Transaction>>(endpoint);
  }
}

export const transactionApi = new TransactionApi();
