import { apiClient } from './api';
import type { ApiResponse, Transaction } from '../types/api';

class TransactionApi {
  async getById(id: string): Promise<ApiResponse<Transaction>> {
    return apiClient.get<Transaction>(`/transactions/${id}`);
  }
}

export const transactionApi = new TransactionApi();
