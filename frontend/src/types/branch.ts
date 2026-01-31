import type { Organization, CreateOrgRequest, UpdateOrgRequest } from './api';
import { OrgType, OrgStatus } from './api';

export { OrgType, OrgStatus };

// Branch type aliases (영업점 = Organization)
export type BranchType = OrgType;

// Business entity type
export enum BusinessType {
  CORPORATION = 'CORPORATION',     // 법인사업자
  INDIVIDUAL = 'INDIVIDUAL',       // 개인사업자
  NON_BUSINESS = 'NON_BUSINESS'    // 비사업자
}

// Extended business info for UI forms
export interface BusinessInfo {
  businessType: BusinessType;
  businessNumber?: string;          // 사업자등록번호
  corporateNumber?: string;         // 법인등록번호
  representative: string;           // 대표자
  openDate?: string;                // 개업연월일
  businessAddress: string;          // 사업장 소재지
  actualAddress?: string;           // 실사업장 소재지
  businessCategory?: string;        // 업태
  businessType2?: string;           // 업종
  mainPhone?: string;               // 대표번호
  managerName?: string;             // 담당자
  managerPhone?: string;            // 담당자 연락처
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
  general: number;    // 일반 수수료율
  small: number;      // 영세 수수료율
  medium1: number;    // 중소1 수수료율
  medium2: number;    // 중소2 수수료율
  medium3: number;    // 중소3 수수료율
  foreign: number;    // 해외카드 수수료율
}

// Transaction limits
export interface LimitConfig {
  perTransaction: number;  // 1회 한도 (백만원 단위)
  perDay: number;          // 1일 한도 (백만원 단위)
}

// Extended Branch type for UI
export interface Branch extends Organization {
  businessInfo?: BusinessInfo;
  bankAccount?: BankAccountInfo;
  feeConfig?: {
    terminal?: FeeConfig;     // 단말기
    oldAuth?: FeeConfig;      // 구인증
    nonAuth?: FeeConfig;      // 비인증
    authPay?: FeeConfig;      // 인증결제
    recurring?: FeeConfig;    // 정기과금
  };
  limitConfig?: LimitConfig;
  createdAt?: string;
  updatedAt?: string;
}

// Create request for wizard
export interface BranchCreateRequest extends CreateOrgRequest {
  businessInfo?: BusinessInfo;
  bankAccount?: BankAccountInfo;
  feeConfig?: Branch['feeConfig'];
  limitConfig?: LimitConfig;
}

// Update request
export interface BranchUpdateRequest extends UpdateOrgRequest {
  businessInfo?: BusinessInfo;
  bankAccount?: BankAccountInfo;
  feeConfig?: Branch['feeConfig'];
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

// Korean labels for branch types
export const BRANCH_TYPE_LABELS: Record<OrgType, string> = {
  [OrgType.DISTRIBUTOR]: '대리점',
  [OrgType.AGENCY]: '에이전시',
  [OrgType.DEALER]: '딜러',
  [OrgType.SELLER]: '셀러',
  [OrgType.VENDOR]: '벤더'
};

// Branch code prefixes
export const BRANCH_CODE_PREFIX: Record<OrgType, string> = {
  [OrgType.DISTRIBUTOR]: 'DI-',
  [OrgType.AGENCY]: 'AG-',
  [OrgType.DEALER]: 'DE-',
  [OrgType.SELLER]: 'SE-',
  [OrgType.VENDOR]: 'VE-'
};
