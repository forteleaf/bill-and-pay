<script lang="ts">
  import { apiClient } from '../lib/api';
  import { authStore } from '../lib/authStore';
  import { tenantStore } from '../lib/stores';
  import type { AuthResponse } from '../types/api';

  let username = $state('');
  let password = $state('');
  let error = $state<string | null>(null);
  let loading = $state(false);

  async function handleLogin(e: Event) {
    e.preventDefault();
    
    if (!username || !password) {
      error = '사용자명과 비밀번호를 입력하세요.';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await apiClient.post<AuthResponse>('/auth/login', { 
        username, 
        password 
      });

      if (response.success && response.data) {
        authStore.login(
          response.data.accessToken,
          response.data.refreshToken,
          response.data.username,
          response.data.tenantId
        );

        tenantStore.setCurrent(response.data.tenantId);
        
        window.location.reload();
      } else {
        error = response.error?.message || '로그인에 실패했습니다.';
      }
    } catch (err) {
      console.error('Login error:', err);
      error = err instanceof Error ? err.message : '로그인 중 오류가 발생했습니다.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="login-container">
  <div class="login-card">
    <div class="login-header">
      <h1>Bill&Pay</h1>
      <p>정산 플랫폼</p>
    </div>

    <form onsubmit={handleLogin}>
      <div class="form-group">
        <label for="username">사용자명</label>
        <input
          id="username"
          type="text"
          bind:value={username}
          placeholder="사용자명을 입력하세요"
          disabled={loading}
          autocomplete="username"
        />
      </div>

      <div class="form-group">
        <label for="password">비밀번호</label>
        <input
          id="password"
          type="password"
          bind:value={password}
          placeholder="비밀번호를 입력하세요"
          disabled={loading}
          autocomplete="current-password"
        />
      </div>

      {#if error}
        <div class="error-message">
          {error}
        </div>
      {/if}

      <button type="submit" disabled={loading} class="login-button">
        {loading ? '로그인 중...' : '로그인'}
      </button>
    </form>
  </div>
</div>

<style>
  .login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 1rem;
  }

  .login-card {
    background: white;
    border-radius: 12px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
    padding: 3rem;
    width: 100%;
    max-width: 420px;
  }

  .login-header {
    text-align: center;
    margin-bottom: 2.5rem;
  }

  .login-header h1 {
    font-size: 2rem;
    font-weight: 700;
    color: #1a202c;
    margin: 0 0 0.5rem 0;
  }

  .login-header p {
    font-size: 0.95rem;
    color: #718096;
    margin: 0;
  }

  form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  label {
    font-size: 0.875rem;
    font-weight: 600;
    color: #2d3748;
  }

  input {
    padding: 0.75rem 1rem;
    font-size: 1rem;
    border: 2px solid #e2e8f0;
    border-radius: 8px;
    transition: all 0.2s;
    outline: none;
  }

  input:focus {
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }

  input:disabled {
    background-color: #f7fafc;
    cursor: not-allowed;
  }

  .error-message {
    padding: 0.875rem;
    background-color: #fed7d7;
    border: 1px solid #fc8181;
    border-radius: 8px;
    color: #c53030;
    font-size: 0.875rem;
    text-align: center;
  }

  .login-button {
    padding: 0.875rem;
    font-size: 1rem;
    font-weight: 600;
    color: white;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
  }

  .login-button:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
  }

  .login-button:active:not(:disabled) {
    transform: translateY(0);
  }

  .login-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  @media (max-width: 640px) {
    .login-card {
      padding: 2rem;
    }

    .login-header h1 {
      font-size: 1.75rem;
    }
  }
</style>
