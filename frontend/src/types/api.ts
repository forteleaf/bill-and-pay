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
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface BusinessEntityDto {
  id: string;
  businessType: string;
  businessNumber?: string;
  corporateNumber?: string;
  businessName: string;
  representativeName: string;
  openDate?: string;
  businessAddress?: string;
  actualAddress?: string;
  businessCategory?: string;
  businessSubCategory?: string;
  mainPhone?: string;
  managerName?: string;
  managerPhone?: string;
  email?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Organization {
  id: string;
  orgCode: string;
  name: string;
  orgType: string;
  path: string;
  level: number;
  status: string;
  businessEntityId?: string;
  businessEntity?: BusinessEntityDto;
  email?: string;
  phone?: string;
  address?: string;
  createdAt?: string;
  updatedAt?: string;
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
  transactionId: string;
  merchantId: string;
  merchantPath: string;
  orgPath: string;
  paymentMethodId: string;
  cardCompanyId?: string;
  amount: number;
  currency: string;
  status: string;
  pgTransactionId: string;
  approvalNumber?: string;
  approvedAt?: string;
  cancelledAt?: string;
  catId: string;
  tid: string;
  metadata?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

export interface Settlement {
  id: string;
  settlementBatchId?: string;
  transactionEventId: string;
  transactionId: string;
  merchantId: string;
  merchantPath: string;
  entityId: string;
  entityType: string;
  entityPath: string;
  entryType: string;
  amount: number;
  feeAmount: number;
  netAmount: number;
  currency: string;
  feeRate: number;
  feeConfig?: Record<string, any>;
  status: string;
  settledAt?: string;
  metadata?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  tenantId: string;
}

export interface SettlementBatch {
  id: string;
  batchNumber: string;
  settlementDate: string;
  status: string;
  totalTransactions: number;
  totalAmount: number;
  totalFeeAmount: number;
  processedAt: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SettlementSummary {
  entityId: string;
  entityType: string;
  entityPath: string;
  totalAmount: number;
  totalFeeAmount: number;
  totalNetAmount: number;
  creditAmount: number;
  debitAmount: number;
  transactionCount: number;
  currency: string;
}

export interface MerchantOrgHistory {
  id: string;
  merchantId: string;
  fromOrgId: string;
  fromOrgPath: string;
  toOrgId: string;
  toOrgPath: string;
  movedBy: string;
  reason?: string;
  movedAt: string;
}

// Organization Management Types
export enum OrgType {
  DISTRIBUTOR = 'DISTRIBUTOR',
  AGENCY = 'AGENCY',
  DEALER = 'DEALER',
  SELLER = 'SELLER',
  VENDOR = 'VENDOR'
}

export enum OrgStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  TERMINATED = 'TERMINATED'
}

export interface CreateOrgRequest {
  orgCode?: string;
  name: string;
  orgType: OrgType;
  parentId?: string;
  businessEntityId: string;
  email?: string;
  phone?: string;
  address?: string;
}

export interface UpdateOrgRequest {
  name?: string;
  status?: OrgStatus;
  businessEntityId?: string;
  email?: string;
  phone?: string;
  address?: string;
}

export interface OrgTree extends Organization {
  children: OrgTree[];
}

export interface PreferentialBusinessRequest {
  businessNumbers: string;
}

export interface PreferentialBusinessResponse {
  businessNumber: string;
  smeCategoryGrade: string;
  firstHalf2025Grade: string;
  secondHalf2025Grade: string;
}
