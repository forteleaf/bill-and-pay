export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: {
    code: string;
    message: string;
  };
  meta?: Record<string, any>;
}

export interface PagedResponse<T> {
  success: boolean;
  data: T[];
  meta: {
    page: number;
    size: number;
    total: number;
    hasNext: boolean;
  };
}

export interface Organization {
  id: string;
  orgCode: string;
  name: string;
  orgType: string;
  path: string;
  depth: number;
  status: string;
}

export interface Merchant {
  id: string;
  code: string;
  name: string;
  orgPath: string;
  status: string;
}

export interface Transaction {
  id: string;
  pgTid: string;
  merchantId: string;
  originalAmount: number;
  currentAmount: number;
  status: string;
  transactedAt: string;
}

export interface Settlement {
  id: string;
  transactionEventId: string;
  entityType: string;
  entityId: string;
  entryType: string;
  amount: number;
  settlementStatus: string;
}
