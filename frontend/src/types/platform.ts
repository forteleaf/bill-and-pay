export interface PlatformAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  username: string;
  role: string;
  fullName: string;
}

export interface TenantDto {
  id: string;
  name: string;
  schemaName: string;
  status: string;
  contactEmail?: string;
  contactPhone?: string;
  config?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformDashboard {
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  provisioningTenants: number;
  totalAuthUsers: number;
  totalPgConnections: number;
}

export interface PlatformAdminDto {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phone?: string;
  role: string;
  twoFactorEnabled: boolean;
  lastLoginAt?: string;
  failedLoginCount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformAnnouncementDto {
  id: string;
  title: string;
  content: string;
  announcementType: string;
  severity: string;
  targetType: string;
  targetTenantIds?: string[];
  status: string;
  publishAt?: string;
  expireAt?: string;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlatformAuditLogDto {
  id: string;
  adminId: string;
  adminUsername: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  targetTenantId?: string;
  details?: Record<string, any>;
  ipAddress?: string;
  createdAt: string;
}

export interface TenantCreateRequest {
  tenantId: string;
  name: string;
  contactEmail?: string;
  contactPhone?: string;
  adminUsername: string;
  adminPassword: string;
  adminEmail?: string;
}

export interface PgConnectionDto {
  id: number;
  pgCode: string;
  pgName: string;
  apiBaseUrl: string;
  webhookBaseUrl?: string;
  tenantId?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}
