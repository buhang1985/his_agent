<template>
  <div class="rich-editor-wrapper" :class="{ 'is-focused': isFocused, 'has-error': error }">
    <editor-content :editor="editor" class="rich-editor-content" />
    
    <div v-if="error" class="editor-error">
      <el-icon><warning /></el-icon>
      <span>{{ error }}</span>
    </div>
    
    <div v-if="showCharCount" class="char-count">
      {{ charCount }} 字
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount, watch } from 'vue';
import { useEditor, EditorContent } from '@tiptap/vue-3';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';
import CharacterCount from '@tiptap/extension-character-count';

interface Props {
  modelValue: string;
  placeholder?: string;
  maxLength?: number;
  showCharCount?: boolean;
  error?: string;
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入内容...',
  maxLength: 10000,
  showCharCount: false,
  error: '',
  disabled: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'change': [value: string];
}>();

const isFocused = ref(false);
const charCount = ref(0);

const editor = useEditor({
  content: props.modelValue,
  editable: !props.disabled,
  extensions: [
    StarterKit.configure({
      bulletList: {
        keepMarks: true,
        keepAttributes: false,
      },
      orderedList: {
        keepMarks: true,
        keepAttributes: false,
      },
    }),
    Placeholder.configure({
      placeholder: props.placeholder,
    }),
    CharacterCount.configure({
      limit: props.maxLength,
    }),
  ],
  onUpdate: ({ editor }) => {
    const content = editor.getHTML();
    const text = editor.state.doc.textContent;
    charCount.value = text.length;
    emit('update:modelValue', content);
    emit('change', content);
  },
  onFocus: () => {
    isFocused.value = true;
  },
  onBlur: () => {
    isFocused.value = false;
  },
});

watch(() => props.modelValue, (value) => {
  const isSame = editor.value?.getHTML() === value;
  if (isSame) return;
  editor.value?.commands.setContent(value, false);
});

watch(() => props.disabled, (disabled) => {
  editor.value?.setEditable(!disabled);
});

onBeforeUnmount(() => {
  editor.value?.destroy();
});

defineExpose({
  focus: () => editor.value?.chain().focus().run(),
  setContent: (content: string) => editor.value?.commands.setContent(content),
  getContent: () => editor.value?.getHTML(),
  getText: () => editor.value?.getText(),
  clear: () => editor.value?.commands.setContent(''),
});
</script>

<style scoped>
.rich-editor-wrapper {
  position: relative;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 12px 16px;
  transition: border-color 0.2s;
  background: #fff;
}

.rich-editor-wrapper.is-focused {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.rich-editor-wrapper.has-error {
  border-color: #F56C6C;
}

.rich-editor-content :deep(.ProseMirror) {
  outline: none;
  min-height: 100px;
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.8;
}

.rich-editor-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  color: #c0c4cc;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}

.rich-editor-content :deep(.ProseMirror ul) {
  padding-left: 1.5em;
}

.rich-editor-content :deep(.ProseMirror ol) {
  padding-left: 1.5em;
}

.editor-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #F56C6C;
  font-size: 12px;
  margin-top: 8px;
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
