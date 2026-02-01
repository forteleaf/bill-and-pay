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

// Korean Bank Codes
export const KOREAN_BANK_CODES: Record<string, string> = {
	'004': 'KB국민은행',
	'011': 'NH농협은행',
	'020': '우리은행',
	'088': '신한은행',
	'081': '하나은행',
	'071': '우체국',
	'030': '대구은행',
	'031': '부산은행',
	'032': '광주은행',
	'034': '전북은행',
	'035': '제주은행',
	'039': '경남은행',
	'050': '저축은행',
	'089': '카카오뱅크',
	'090': '토스뱅크',
	'027': '씨티은행',
	'037': '전주은행',
	'040': '경주은행',
	'045': '새마을금고',
	'048': '신협',
	'060': '기업은행',
	'062': '광주신용금고',
	'064': '산림조합',
	'065': '해양수산신협',
	'066': '수협',
	'067': '신용보증기금',
	'068': '기술보증기금',
	'069': '한국주택금융공사',
	'070': '우리금융투자',
	'072': '한국투자증권',
	'073': '삼성증권',
	'074': '대우증권',
	'075': '미래에셋증권',
	'076': '메리츠증권',
	'077': '키움증권',
	'078': '이베스트투자증권',
	'079': '하이투자증권',
	'080': '하나금융투자',
	'082': '신한금융투자',
	'083': '현대차증권',
	'084': '투자',
	'085': '삼성금융투자',
	'086': '한국금융투자',
	'087': '한국증권금융',
	'091': '카카오페이증권',
	'092': '토스증권'
};

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

// Helper function to get bank name from code
export function getBankName(bankCode: string): string {
	return KOREAN_BANK_CODES[bankCode] || bankCode;
}

// Helper function to mask account number
export function maskAccountNumber(accountNumber: string): string {
	if (!accountNumber || accountNumber.length < 4) {
		return accountNumber;
	}
	const lastFour = accountNumber.slice(-4);
	const masked = '*'.repeat(accountNumber.length - 4);
	return masked + lastFour;
}

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
