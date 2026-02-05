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
  merchantCount?: number;
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
  merchantName?: string;
  orgPath: string;
  paymentMethodId: string;
  cardCompanyId?: string;
  pgConnectionId?: number;
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
  orgPath: string;
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
  businessEntityId?: string;
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

export interface OrganizationSettlementSummary {
  organizationId: string;
  orgCode: string;
  orgName: string;
  orgType: string;
  orgPath: string;
  level: number;
  businessEntityId?: string;
  representativeName?: string;
  mainPhone?: string;
  merchantCount: number;
  approvalAmount: number;
  approvalCount: number;
  cancelAmount: number;
  cancelCount: number;
  netPaymentAmount: number;
  totalTransactionCount: number;
  merchantFeeAmount: number;
  orgFeeAmount: number;
  avgFeeRate: number;
  primaryStatus?: string;
  completedCount: number;
  pendingCount: number;
  failedCount: number;
  currency: string;
}

export interface MerchantSettlement {
  merchantId: string;
  merchantCode: string;
  merchantName: string;
  transactionDate: string;
  branchName: string;
  approvalAmount: number;
  approvalCount: number;
  cancelAmount: number;
  cancelCount: number;
  netPaymentAmount: number;
  feeAmount: number;
  feeRate: number;
}

export interface HierarchyFee {
  entityType: string;
  entityName: string;
  entityCode: string;
  feeRate: number;
  feeAmount: number;
  netAmount: number;
}

export interface SettlementCalculation {
  settlementFeeRate: number;
  grossAmount: number;
  supplyAmount: number;
  vatAmount: number;
  finalAmount: number;
  childOrgFeeAmount: number;
  merchantPayoutAmount: number;
}

export interface OrganizationSettlementDetail {
  organizationId: string;
  orgCode: string;
  orgName: string;
  orgType: string;
  orgPath: string;
  summary: OrganizationSettlementSummary;
  merchantSettlements: MerchantSettlement[];
  hierarchyFees: HierarchyFee[];
  calculation: SettlementCalculation;
}
