export enum FeeType {
	PERCENTAGE = 'PERCENTAGE',
	FIXED = 'FIXED',
	TIERED = 'TIERED'
}

export enum FeeConfigStatus {
	ACTIVE = 'ACTIVE',
	INACTIVE = 'INACTIVE',
	PENDING = 'PENDING',
	EXPIRED = 'EXPIRED'
}

export interface FeeConfigurationDto {
	id: string;
	entityId: string;
	entityType: string;
	entityPath: string;
	paymentMethodId: string;
	paymentMethodName?: string;
	cardCompanyId?: string;
	feeType: FeeType;
	feeRate: number;
	fixedFee?: number;
	tierConfig?: Record<string, any>;
	minFee?: number;
	maxFee?: number;
	priority: number;
	validFrom: string;
	validUntil?: string;
	status: FeeConfigStatus;
	metadata?: Record<string, any>;
	createdAt: string;
	updatedAt: string;
}

export interface FeeConfigurationCreateRequest {
	paymentMethodId: string;
	feeType?: FeeType;
	feeRate?: number;
	fixedFee?: number;
	minFee?: number;
	maxFee?: number;
	priority?: number;
	validFrom?: string;
	validUntil?: string;
	reason?: string;
}

export interface FeeConfigurationUpdateRequest {
	paymentMethodId?: string;
	feeType?: FeeType;
	feeRate?: number;
	fixedFee?: number;
	minFee?: number;
	maxFee?: number;
	priority?: number;
	validFrom?: string;
	validUntil?: string;
	status?: FeeConfigStatus;
	reason: string;
}

export interface FeeConfigHistoryDto {
	id: string;
	feeConfigurationId: string;
	merchantId?: string;
	action: string;
	fieldName?: string;
	oldValue?: string;
	newValue?: string;
	oldFeeRate?: number;
	newFeeRate?: number;
	oldStatus?: string;
	newStatus?: string;
	reason?: string;
	changedBy: string;
	changedAt: string;
	createdAt: string;
}

export const FEE_TYPE_LABELS: Record<FeeType, string> = {
	[FeeType.PERCENTAGE]: '정률',
	[FeeType.FIXED]: '정액',
	[FeeType.TIERED]: '구간별'
};

export const FEE_CONFIG_STATUS_LABELS: Record<FeeConfigStatus, string> = {
	[FeeConfigStatus.ACTIVE]: '활성',
	[FeeConfigStatus.INACTIVE]: '비활성',
	[FeeConfigStatus.PENDING]: '대기',
	[FeeConfigStatus.EXPIRED]: '만료'
};

export const FEE_HISTORY_ACTION_LABELS: Record<string, string> = {
	CREATE: '등록',
	UPDATE: '수정',
	DEACTIVATE: '비활성화',
	ACTIVATE: '활성화'
};

export const FEE_HISTORY_FIELD_LABELS: Record<string, string> = {
	fee_rate: '수수료율',
	fee_type: '수수료유형',
	status: '상태',
	valid_from: '시작일',
	valid_until: '종료일',
	payment_method: '결제수단',
	fixed_fee: '고정수수료',
	min_fee: '최소수수료',
	max_fee: '최대수수료',
	priority: '우선순위'
};
