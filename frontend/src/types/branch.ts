import type { Organization, CreateOrgRequest, UpdateOrgRequest, BusinessEntityDto } from "./api";
import { OrgType, OrgStatus } from "./api";

export { OrgType, OrgStatus };
export type { BusinessEntityDto };

// Branch type aliases (영업점 = Organization)
export type BranchType = OrgType;

export enum BusinessType {
	INDIVIDUAL = "INDIVIDUAL",
	CORPORATION = "CORPORATION",
	NON_BUSINESS = "NON_BUSINESS",
}

export interface BusinessEntity {
	id: string;
	businessType: BusinessType;
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

export interface BusinessEntityCreateRequest {
	businessType: BusinessType;
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
}

export interface BusinessInfo {
	businessType: BusinessType;
	businessNumber?: string;
	corporateNumber?: string;
	representative: string;
	openDate?: string;
	businessAddress: string;
	actualAddress?: string;
	businessCategory?: string;
	businessType2?: string;
	mainPhone?: string;
	managerName?: string;
	managerPhone?: string;
	email?: string;
}

// Bank account info
export interface BankAccountInfo {
	bankCode: string;
	bankName: string;
	accountNumber: string;
	accountHolder: string;
}

// Fee configuration by payment type
export interface FeeConfig {
	general: number; // 일반 수수료율
	small: number; // 영세 수수료율
	medium1: number; // 중소1 수수료율
	medium2: number; // 중소2 수수료율
	medium3: number; // 중소3 수수료율
	foreign: number; // 해외카드 수수료율
}

// Transaction limits
export interface LimitConfig {
	perTransaction: number; // 1회 한도 (백만원 단위)
	perDay: number; // 1일 한도 (백만원 단위)
}

// Extended Branch type for UI
export interface Branch extends Organization {
	bankAccount?: BankAccountInfo;
	feeConfig?: {
		terminal?: FeeConfig;
		oldAuth?: FeeConfig;
		nonAuth?: FeeConfig;
		authPay?: FeeConfig;
		recurring?: FeeConfig;
	};
	limitConfig?: LimitConfig;
}

// Create request for wizard
export interface BranchCreateRequest extends CreateOrgRequest {
	bankAccount?: BankAccountInfo;
	feeConfig?: Branch["feeConfig"];
	limitConfig?: LimitConfig;
}

// Update request
export interface BranchUpdateRequest extends UpdateOrgRequest {
	bankAccount?: BankAccountInfo;
	feeConfig?: Branch["feeConfig"];
	limitConfig?: LimitConfig;
}

// List filter params
export interface BranchListParams {
	page?: number;
	size?: number;
	type?: BranchType;
	status?: OrgStatus;
	search?: string;
	startDate?: string;
	endDate?: string;
}

export const ORG_STATUS_LABELS: Record<OrgStatus, string> = {
	[OrgStatus.ACTIVE]: "활성",
	[OrgStatus.SUSPENDED]: "정지",
	[OrgStatus.TERMINATED]: "해지",
};

// Korean labels for branch types
export const BRANCH_TYPE_LABELS: Record<OrgType, string> = {
	[OrgType.DISTRIBUTOR]: "대리점",
	[OrgType.AGENCY]: "에이전시",
	[OrgType.DEALER]: "딜러",
	[OrgType.SELLER]: "셀러",
	[OrgType.VENDOR]: "벤더",
};

// Branch code prefixes
export const BRANCH_CODE_PREFIX: Record<OrgType, string> = {
	[OrgType.DISTRIBUTOR]: "DI-",
	[OrgType.AGENCY]: "AG-",
	[OrgType.DEALER]: "DE-",
	[OrgType.SELLER]: "SE-",
	[OrgType.VENDOR]: "VE-",
};
