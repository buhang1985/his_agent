/**
 * 防重复提交指令
 * 使用方式：v-prevent-submit
 */

import type { Directive, DirectiveBinding } from 'vue';

interface HTMLElementWithDisabled extends HTMLElement {
  _disabled?: boolean;
  _timeout?: ReturnType<typeof setTimeout>;
}

export const preventSubmit: Directive<HTMLElementWithDisabled> = {
  mounted(el: HTMLElementWithDisabled, binding: DirectiveBinding) {
    const duration = binding.arg ? parseInt(binding.arg) : 2000; // 默认 2 秒
    
    el.addEventListener('click', () => {
      if (el._disabled) {
        return;
      }
      
      el._disabled = true;
      
      // 恢复按钮
      if (el._timeout) {
        clearTimeout(el._timeout);
      }
      
      el._timeout = setTimeout(() => {
        el._disabled = false;
      }, duration);
    });
  },
  
  beforeUnmount(el: HTMLElementWithDisabled) {
    if (el._timeout) {
      clearTimeout(el._timeout);
    }
  }
};
