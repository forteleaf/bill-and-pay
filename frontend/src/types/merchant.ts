import { BusinessType } from "./branch";

export { BusinessType as MerchantBusinessType };

export enum MerchantStatus {
	ACTIVE = "ACTIVE",
	SUSPENDED = "SUSPENDED",
	TERMINATED = "TERMINATED",
}

export const MERCHANT_BUSINESS_TYPE_LABELS: Record<BusinessType, string> = {
	[BusinessType.INDIVIDUAL]: "개인사업자",
	[BusinessType.CORPORATION]: "법인사업자",
	[BusinessType.NON_BUSINESS]: "비사업자",
};

export interface AccessibleOrganization {
	id: string;
	code: string;
	name: string;
	type: string;
	path: string;
}

export interface BlacklistCheckResponse {
	isBlacklisted: boolean;
	reason?: string;
	checkedAt: string;
}

export interface MerchantCreateRequest {
	merchantCode: string;
	name: string;
	organizationId: string;
	businessNumber?: string;
	businessType?: BusinessType;
	corporateNumber?: string;
	representative?: string;
	address?: string;
	contactName?: string;
	contactEmail?: string;
	contactPhone?: string;
	config?: Record<string, unknown>;
}

export enum ContactRole {
	PRIMARY = "PRIMARY",
	SECONDARY = "SECONDARY",
	SETTLEMENT = "SETTLEMENT",
	TECHNICAL = "TECHNICAL",
}

export interface ContactDto {
	id: string;
	name: string;
	phone?: string;
	email?: string;
	role: ContactRole;
	entityType: string;
	entityId: string;
	isPrimary: boolean;
	createdAt: string;
	updatedAt: string;
}

export interface MerchantDto {
	id: string;
	merchantCode: string;
	name: string;
	organizationId: string;
	organizationName?: string;
	businessNumber?: string;
	businessType?: BusinessType;
	address?: string;
	status: MerchantStatus;
	config?: Record<string, unknown>;
	createdAt: string;
	updatedAt: string;
	primaryContact?: ContactDto;
	contacts?: ContactDto[];
}

export interface MerchantListParams {
	page?: number;
	size?: number;
	status?: MerchantStatus;
	businessType?: BusinessType;
	organizationId?: string;
	search?: string;
}

export interface MerchantUpdateRequest {
	name?: string;
	businessNumber?: string;
	businessType?: BusinessType;
	contactName?: string;
	contactEmail?: string;
	contactPhone?: string;
	address?: string;
	status?: MerchantStatus;
	config?: Record<string, unknown>;
}

/**
 * Contact API Request Types
 */
export interface ContactCreateRequest {
	name: string;
	phone?: string;
	email?: string;
	role: ContactRole;
	entityType: string;
	entityId: string;
}

export interface ContactUpdateRequest {
	name?: string;
	phone?: string;
	email?: string;
	role?: ContactRole;
}
