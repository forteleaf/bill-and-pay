interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  username: string | null;
  tenantId: string | null;
  isAuthenticated: boolean;
}

class AuthStore {
  private state: AuthState = {
    accessToken: null,
    refreshToken: null,
    username: null,
    tenantId: null,
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

      if (accessToken) {
        this.state = {
          accessToken,
          refreshToken,
          username,
          tenantId,
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
      isAuthenticated: true,
    };

    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('username', username);
      localStorage.setItem('tenantId', tenantId);
    }
  }

  logout() {
    this.state = {
      accessToken: null,
      refreshToken: null,
      username: null,
      tenantId: null,
      isAuthenticated: false,
    };

    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('username');
      localStorage.removeItem('tenantId');
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

  isAuthenticated(): boolean {
    return this.state.isAuthenticated;
  }
}

export const authStore = new AuthStore();
