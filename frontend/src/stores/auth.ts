interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  username: string | null;
  tenantId: string | null;
  role: string | null;
  fullName: string | null;
  isAuthenticated: boolean;
}

class AuthStore {
  private state: AuthState = {
    accessToken: null,
    refreshToken: null,
    username: null,
    tenantId: null,
    role: null,
    fullName: null,
    isAuthenticated: false,
  };

  constructor() {
    this.loadFromStorage();
  }

  private loadFromStorage() {
    if (typeof window !== 'undefined') {
      const accessToken = localStorage.getItem('accessToken');
      const refreshToken = localStorage.getItem('refreshToken');
      const username = localStorage.getItem('username');
      const tenantId = localStorage.getItem('tenantId');
      const role = localStorage.getItem('role');
      const fullName = localStorage.getItem('fullName');

      if (accessToken) {
        this.state = {
          accessToken,
          refreshToken,
          username,
          tenantId,
          role,
          fullName,
          isAuthenticated: true,
        };
      }
    }
  }

  login(
    accessToken: string,
    refreshToken: string,
    username: string,
    tenantId: string
  ) {
    this.state = {
      accessToken,
      refreshToken,
      username,
      tenantId,
      role: null,
      fullName: null,
      isAuthenticated: true,
    };

    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('username', username);
      localStorage.setItem('tenantId', tenantId);
      localStorage.removeItem('role');
      localStorage.removeItem('fullName');
    }
  }

  platformLogin(
    accessToken: string,
    refreshToken: string,
    username: string,
    role: string,
    fullName: string
  ) {
    this.state = {
      accessToken,
      refreshToken,
      username,
      tenantId: null,
      role,
      fullName,
      isAuthenticated: true,
    };

    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('username', username);
      localStorage.setItem('role', role);
      localStorage.setItem('fullName', fullName);
      localStorage.removeItem('tenantId');
    }
  }

  logout() {
    this.state = {
      accessToken: null,
      refreshToken: null,
      username: null,
      tenantId: null,
      role: null,
      fullName: null,
      isAuthenticated: false,
    };

    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('username');
      localStorage.removeItem('tenantId');
      localStorage.removeItem('role');
      localStorage.removeItem('fullName');
    }
  }

  updateTokens(accessToken: string, refreshToken: string) {
    this.state.accessToken = accessToken;
    this.state.refreshToken = refreshToken;

    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
    }
  }

  getAccessToken(): string | null {
    return this.state.accessToken;
  }

  getRefreshToken(): string | null {
    return this.state.refreshToken;
  }

  getTenantId(): string | null {
    return this.state.tenantId;
  }

  getUsername(): string | null {
    return this.state.username;
  }

  getRole(): string | null {
    return this.state.role;
  }

  getFullName(): string | null {
    return this.state.fullName;
  }

  isAuthenticated(): boolean {
    return this.state.isAuthenticated;
  }

  isPlatformAdmin(): boolean {
    const role = this.state.role;
    return role === 'SUPER_ADMIN' || role === 'PLATFORM_OPERATOR' || role === 'PLATFORM_VIEWER';
  }
}

export const authStore = new AuthStore();
