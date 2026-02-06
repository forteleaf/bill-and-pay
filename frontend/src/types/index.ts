/**
 * 타입 시스템 중앙 export 파일
 * 모든 타입을 여기서 re-export하여 일관된 import 경로 제공
 */

// Core API Types
export type { ApiResponse, PagedResponse } from './api';

// Domain Types - API
export type {
  BusinessEntityDto,
  Organization,
  Merchant,
  Transaction,
  Settlement,
  SettlementBatch,
  SettlementSummary,
  AuthResponse,
  MerchantOrgHistory,
  OrganizationSettlementSummary,
  MerchantSettlement,
  HierarchyFee,
  SettlementCalculation,
  OrganizationSettlementDetail
} from './api';

// Enums - API
export { OrgType, OrgStatus } from './api';

// Organization Management
export type {
  CreateOrgRequest,
  UpdateOrgRequest,
  OrgTree,
  PreferentialBusinessRequest,
  PreferentialBusinessResponse
} from './api';

// Branch (Organization) Types
export type {
  BusinessEntity,
  BusinessEntityCreateRequest,
  BusinessInfo,
  BankAccountInfo,
  FeeConfig,
  LimitConfig,
  Branch,
  BranchCreateRequest,
  BranchUpdateRequest,
  BranchListParams
} from './branch';

export { BusinessType, BRANCH_TYPE_LABELS, BRANCH_CODE_PREFIX } from './branch';
export type { BranchType } from './branch';

// Merchant Types
export type {
  AccessibleOrganization,
  BlacklistCheckResponse,
  MerchantCreateRequest,
  ContactDto,
  ContactCreateRequest,
  ContactUpdateRequest,
  MerchantDto,
  MerchantListParams,
  MerchantUpdateRequest
} from './merchant';

export { MerchantStatus, ContactRole, MERCHANT_BUSINESS_TYPE_LABELS } from './merchant';
export type { MerchantBusinessType } from './merchant';

// User Types
export type {
  UserPermissions,
  UserDto,
  UserCreateRequest,
  UserUpdateRequest,
  PasswordChangeRequest,
  UserListParams
} from './user';

export { UserStatus, UserRole, USER_STATUS_LABELS, USER_ROLE_LABELS } from './user';

// Terminal Types
export type {
  TerminalDto,
  TerminalListParams,
  TerminalCreateRequest,
  TerminalUpdateRequest
} from './terminal';

export { TerminalStatus, TerminalType, TERMINAL_STATUS_LABELS, TERMINAL_TYPE_LABELS } from './terminal';

// PG Connection Types
export type {
  PgConnectionDto,
  PgConnectionCreateRequest,
  PgConnectionUpdateRequest
} from './pgConnection';

export { PgConnectionStatus, PG_CONNECTION_STATUS_LABELS } from './pgConnection';

// Merchant PG Mapping Types
export type {
  MerchantPgMappingDto,
  MerchantPgMappingCreateRequest,
  MerchantPgMappingUpdateRequest
} from './merchantPgMapping';

export { MerchantPgMappingStatus, MERCHANT_PG_MAPPING_STATUS_LABELS } from './merchantPgMapping';

// Settlement Account Types
export type {
  SettlementAccountDto,
  SettlementAccountCreateRequest,
  SettlementAccountUpdateRequest,
  SettlementAccountListParams,
  SettlementAccountVerifyRequest,
  SettlementAccountPrimaryResponse
} from './settlementAccount';

export {
  SettlementAccountStatus,
  SettlementAccountEntityType,
  SETTLEMENT_ACCOUNT_STATUS_LABELS,
  SETTLEMENT_ACCOUNT_ENTITY_TYPE_LABELS,
  getBankName,
  maskAccountNumber,
  KOREAN_BANK_CODES
} from './settlementAccount';

// Settlement Types (Request Params)
export type {
  SettlementListParams,
  SettlementSummaryParams,
  SettlementBatchListParams,
  OrganizationSettlementParams
} from './settlement';
