import { apiClient } from './client';
import { buildEndpoint, toApiDateTime } from './queryUtils';
import type { ApiResponse, Transaction, PagedResponse } from '@/types/api';

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
  async getById(id: string): Promise<ApiResponse<Transaction>> {
    return apiClient.get<Transaction>(`/transactions/${id}`);
  }

  async getTransactions(params: TransactionListParams = {}): Promise<ApiResponse<PagedResponse<Transaction>>> {
    return apiClient.get<PagedResponse<Transaction>>(buildEndpoint('/transactions', {
      page: params.page,
      size: params.size,
      status: params.status,
      merchantId: params.merchantId,
      sortBy: params.sortBy,
      direction: params.direction,
      startDate: toApiDateTime(params.startDate, '00:00:00'),
      endDate: toApiDateTime(params.endDate, '23:59:59'),
    }));
  }
}

export const transactionApi = new TransactionApi();
