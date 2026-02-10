// Settlement Account Status
export enum SettlementAccountStatus {
	ACTIVE = 'ACTIVE',
	INACTIVE = 'INACTIVE',
	PENDING_VERIFICATION = 'PENDING_VERIFICATION'
}

// Entity Type for Settlement Accounts
export enum SettlementAccountEntityType {
	BUSINESS_ENTITY = 'BUSINESS_ENTITY',
	MERCHANT = 'MERCHANT'
}

// Settlement Account DTO
export interface SettlementAccountDto {
	id: string;
	bankCode: string;
	bankName: string;
	accountNumber: string;
	accountHolder: string;
	maskedAccountNumber: string;
	status: SettlementAccountStatus;
	statusDescription: string;
	isPrimary: boolean;
	verifiedAt?: string;
	memo?: string;
	entityType: SettlementAccountEntityType;
	entityId: string;
	createdAt: string;
	updatedAt: string;
}

// Create Settlement Account Request
export interface SettlementAccountCreateRequest {
	bankCode: string;
	accountNumber: string;
	accountHolder: string;
	memo?: string;
	entityType: SettlementAccountEntityType;
	entityId: string;
}

// Update Settlement Account Request
export interface SettlementAccountUpdateRequest {
	bankCode?: string;
	accountNumber?: string;
	accountHolder?: string;
	memo?: string;
	status?: SettlementAccountStatus;
}

// Settlement Account List Params
export interface SettlementAccountListParams {
	page?: number;
	size?: number;
	status?: SettlementAccountStatus;
	entityType?: SettlementAccountEntityType;
	entityId?: string;
}

// Settlement Account Verification Request
export interface SettlementAccountVerifyRequest {
	verificationCode?: string;
}

// Settlement Account Primary Response
export interface SettlementAccountPrimaryResponse {
	id: string;
	bankCode: string;
	bankName: string;
	accountNumber: string;
	accountHolder: string;
	maskedAccountNumber: string;
	status: SettlementAccountStatus;
	statusDescription: string;
	isPrimary: boolean;
	verifiedAt?: string;
	memo?: string;
	entityType: SettlementAccountEntityType;
	entityId: string;
	createdAt: string;
	updatedAt: string;
}

// Helper functions and bank codes moved to src/utils/formatters.ts
export { getBankName, maskAccountNumber, KOREAN_BANK_CODES } from '@/utils/formatters';

// Status label mapping
export const SETTLEMENT_ACCOUNT_STATUS_LABELS: Record<SettlementAccountStatus, string> = {
	[SettlementAccountStatus.ACTIVE]: '활성',
	[SettlementAccountStatus.INACTIVE]: '비활성',
	[SettlementAccountStatus.PENDING_VERIFICATION]: '검증 대기중'
};

// Entity type label mapping
export const SETTLEMENT_ACCOUNT_ENTITY_TYPE_LABELS: Record<SettlementAccountEntityType, string> = {
	[SettlementAccountEntityType.BUSINESS_ENTITY]: '사업자',
	[SettlementAccountEntityType.MERCHANT]: '가맹점'
};
