import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface UserInfo {
  id: string;
  username: string;
  email: string;
  role: string;
  fullName: string;
}

export interface AuthState {
  token: string | null;
  user: UserInfo | null;
  isAuthenticated: boolean;
}

/**
 * 用户认证 Store
 * 管理用户登录状态、Token、权限等
 */
export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(localStorage.getItem('auth_token'));
  const user = ref<UserInfo | null>(null);
  const isAuthenticated = computed(() => !!token.value);

  // Actions
  function setToken(newToken: string) {
    token.value = newToken;
    localStorage.setItem('auth_token', newToken);
  }

  function setUser(userInfo: UserInfo) {
    user.value = userInfo;
  }

  function logout() {
    token.value = null;
    user.value = null;
    localStorage.removeItem('auth_token');
  }

  function hasRole(requiredRole: string): boolean {
    if (!user.value) return false;
    return user.value.role === requiredRole || user.value.role === 'ADMIN';
  }

  return {
    // State
    token,
    user,
    isAuthenticated,
    // Actions
    setToken,
    setUser,
    logout,
    hasRole,
  };
});
