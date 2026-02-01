export enum UserStatus {
	ACTIVE = "ACTIVE",
	SUSPENDED = "SUSPENDED",
	DELETED = "DELETED",
}

export enum UserRole {
	ADMIN = "ADMIN",
	MANAGER = "MANAGER",
	VIEWER = "VIEWER",
	API_USER = "API_USER",
}

export const USER_STATUS_LABELS: Record<UserStatus, string> = {
	[UserStatus.ACTIVE]: "활성",
	[UserStatus.SUSPENDED]: "정지",
	[UserStatus.DELETED]: "삭제",
};

export const USER_ROLE_LABELS: Record<UserRole, string> = {
	[UserRole.ADMIN]: "관리자",
	[UserRole.MANAGER]: "매니저",
	[UserRole.VIEWER]: "뷰어",
	[UserRole.API_USER]: "API 사용자",
};

export interface UserPermissions {
	[key: string]: boolean;
}

export interface UserDto {
	id: string;
	username: string;
	email: string;
	orgId: string;
	orgPath: string;
	fullName: string;
	phone?: string;
	role: UserRole;
	permissions: UserPermissions;
	twoFactorEnabled: boolean;
	lastLoginAt?: string;
	passwordChangedAt?: string;
	status: UserStatus;
	createdAt: string;
	updatedAt: string;
}

export interface UserCreateRequest {
	username: string;
	email: string;
	password: string;
	fullName: string;
	phone?: string;
	role: UserRole;
	orgId: string;
	permissions?: UserPermissions;
}

export interface UserUpdateRequest {
	email?: string;
	fullName?: string;
	phone?: string;
	role?: UserRole;
	permissions?: UserPermissions;
	status?: UserStatus;
}

export interface PasswordChangeRequest {
	currentPassword: string;
	newPassword: string;
}

export interface UserListParams {
	page?: number;
	size?: number;
	status?: UserStatus;
	role?: UserRole;
	orgId?: string;
	search?: string;
}
