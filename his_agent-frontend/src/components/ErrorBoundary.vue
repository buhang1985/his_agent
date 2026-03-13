<template>
  <div class="error-boundary">
    <div v-if="hasError" class="error-container">
      <el-result icon="error" :title="errorTitle" :sub-title="errorSubtitle">
        <template #extra>
          <el-button type="primary" @click="handleRetry">重试</el-button>
          <el-button @click="handleGoHome">返回首页</el-button>
        </template>
      </el-result>
    </div>
    <slot v-else></slot>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

interface Props {
  title?: string;
  subtitle?: string;
}

const props = withDefaults(defineProps<Props>(), {
  title: '出错了',
  subtitle: '系统遇到了一些问题，请稍后重试',
});

const hasError = ref(false);
const errorTitle = ref(props.title);
const errorSubtitle = ref(props.subtitle);

// 监听全局错误
const handleError = (event: ErrorEvent) => {
  console.error('Global error:', event.error);
  hasError.value = true;
  errorSubtitle.value = event.message || '未知错误';
  ElMessage.error('系统异常，已记录错误日志');
};

const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
  console.error('Unhandled promise rejection:', event.reason);
  hasError.value = true;
  errorSubtitle.value = '请求失败，请检查网络连接';
  ElMessage.error('网络请求失败');
};

// 注册全局错误监听
watch(
  () => hasError.value,
  (newVal) => {
    if (newVal) {
      window.addEventListener('error', handleError);
      window.addEventListener('unhandledrejection', handleUnhandledRejection);
    } else {
      window.removeEventListener('error', handleError);
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
    }
  },
  { immediate: true }
);

// 重试
const handleRetry = () => {
  hasError.value = false;
  window.location.reload();
};

// 返回首页
const handleGoHome = () => {
  hasError.value = false;
  window.location.href = '/';
};

// 组件卸载时清理
defineExpose({
  reset: () => {
    hasError.value = false;
  },
});
</script>

<style scoped>
.error-boundary {
  min-height: 100%;
}

.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
</style>
