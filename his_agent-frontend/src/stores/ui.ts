import { defineStore } from 'pinia';
import { ref } from 'vue';

/**
 * UI Store
 * 管理全局 UI 状态（侧边栏、模态框、通知等）
 */
export const useUiStore = defineStore('ui', () => {
  // State
  const sidebarCollapsed = ref(false);
  const loading = ref(false);
  const notifications = ref<Array<{
    id: string;
    type: 'success' | 'warning' | 'error' | 'info';
    message: string;
  }>>([]);

  // Actions
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function setSidebarCollapsed(value: boolean) {
    sidebarCollapsed.value = value;
  }

  function setLoading(value: boolean) {
    loading.value = value;
  }

  function addNotification(notification: {
    type: 'success' | 'warning' | 'error' | 'info';
    message: string;
  }) {
    const id = Date.now().toString();
    notifications.value.push({ id, ...notification });
    
    // 5 秒后自动移除
    setTimeout(() => {
      removeNotification(id);
    }, 5000);
  }

  function removeNotification(id: string) {
    const index = notifications.value.findIndex(n => n.id === id);
    if (index > -1) {
      notifications.value.splice(index, 1);
    }
  }

  function clearNotifications() {
    notifications.value = [];
  }

  return {
    // State
    sidebarCollapsed,
    loading,
    notifications,
    // Actions
    toggleSidebar,
    setSidebarCollapsed,
    setLoading,
    addNotification,
    removeNotification,
    clearNotifications,
  };
});
