<template>
  <el-card class="record-section-card" :class="{ 'is-complete': isComplete }">
    <template #header>
      <div class="section-header">
        <div class="section-title">
          <el-icon :size="18"><component :is="icon" /></el-icon>
          <span>{{ title }}</span>
          <el-tag v-if="required" size="small" type="danger" effect="plain">必填</el-tag>
          <el-tag v-if="isComplete" size="small" type="success">已完成</el-tag>
        </div>
        <div class="section-actions">
          <el-button
            v-if="hasWriteback"
            size="small"
            type="primary"
            :loading="writebackLoading"
            @click="handleWriteback"
          >
            <el-icon><upload /></el-icon>
            回写 HIS
          </el-button>
          <el-button
            size="small"
            @click="handleClear"
            :disabled="!content"
          >
            <el-icon><delete /></el-icon>
            清空
          </el-button>
        </div>
      </div>
    </template>
    
    <RichTextEditor
      v-model="content"
      :placeholder="placeholder"
      :error="error"
      :disabled="disabled"
      :show-char-count="true"
      @change="handleChange"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import RichTextEditor from './RichTextEditor.vue';
import { Upload, Delete } from '@element-plus/icons-vue';

interface Props {
  modelValue: string;
  title: string;
  icon: 'Edit' | 'Document' | 'Files' | 'CircleCheck' | 'Setting';
  placeholder: string;
  required?: boolean;
  hasWriteback?: boolean;
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  required: false,
  hasWriteback: true,
  disabled: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'writeback': [section: string, content: string];
  'clear': [section: string];
}>();

const content = ref(props.modelValue);
const writebackLoading = ref(false);
const error = ref('');
const isComplete = ref(false);

watch(() => props.modelValue, (value) => {
  content.value = value;
});

watch(content, (value) => {
  emit('update:modelValue', value);
  isComplete.value = value && value.length > 10;
});

const handleChange = () => {
  error.value = '';
};

const handleWriteback = async () => {
  if (!content.value) {
    error.value = '内容为空，无法回写';
    return;
  }
  
  writebackLoading.value = true;
  try {
    emit('writeback', props.title, content.value);
  } finally {
    writebackLoading.value = false;
  }
};

const handleClear = () => {
  content.value = '';
  emit('update:modelValue', '');
  emit('clear', props.title);
};

defineExpose({
  getContent: () => content.value,
  setContent: (value: string) => {
    content.value = value;
  },
});
</script>

<style scoped>
.record-section-card {
  margin-bottom: 16px;
  transition: all 0.3s;
}

.record-section-card.is-complete {
  border-color: #67C23A;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.section-actions {
  display: flex;
  gap: 8px;
}
</style>
