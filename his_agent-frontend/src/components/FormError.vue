<template>
  <div class="form-error" role="alert" aria-live="polite">
    <el-icon class="form-error__icon"><Warning /></el-icon>
    <span class="form-error__message">{{ message }}</span>
  </div>
</template>

<script setup lang="ts">
import { Warning } from '@element-plus/icons-vue';

defineProps<{
  message: string;
}>();
</script>

<style scoped>
.form-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-color-danger);
}

.form-error__icon {
  font-size: 14px;
  flex-shrink: 0;
}

.form-error__message {
  word-break: break-word;
}
</style>
