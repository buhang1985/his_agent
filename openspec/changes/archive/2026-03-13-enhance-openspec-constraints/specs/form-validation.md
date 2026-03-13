# 表单验证规范

**版本**: 1.0  
**日期**: 2026-03-11  
**状态**: 新增

---

## 新增需求

### 需求：表单验证库选择

必须使用 VeeValidate + Yup 进行表单验证，保证一致性和可维护性。

#### 场景：验证库配置
- **当** 配置表单验证时
- **那么** 必须使用：
```typescript
// VeeValidate + Yup
import { defineRule, configure } from 'vee-validate';
import { required, email, min, max } from '@vee-validate/rules';
import { localize } from '@vee-validate/i18n';
import zh from '@vee-validate/i18n/dist/locale/zh_CN.json';

// 定义全局规则
defineRule('required', required);
defineRule('email', email);

// 自定义规则
defineRule('phone', (value: string) => {
  if (!value) return true;  // 空值由 required 处理
  return /^1[3-9]\d{9}$/.test(value) || '请输入正确的手机号';
});

defineRule('idCard', (value: string) => {
  if (!value) return true;
  return /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/.test(value) 
    || '请输入正确的身份证号';
});

// 配置中文
configure({
  generateMessages: localize({ zh }),
});
```

### 需求：表单验证模式

必须使用 Schema 验证模式，集中管理验证规则。

#### 场景：Yup Schema 定义
- **当** 定义表单验证规则时
- **那么** 必须：
```vue
<script setup lang="ts">
import { object, string, number, date } from 'yup';

const validationSchema = object({
  // 必填字段
  patientName: string()
    .required('患者姓名不能为空')
    .min(2, '姓名至少 2 个字')
    .max(20, '姓名最多 20 个字'),
  
  // 身份证号
  idCard: string()
    .required('身份证号不能为空')
    .matches(
      /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/,
      '身份证号格式不正确'
    ),
  
  // 手机号
  phone: string()
    .required('手机号不能为空')
    .matches(/^1[3-9]\d{9}$/, '手机号格式不正确'),
  
  // 邮箱
  email: string()
    .email('邮箱格式不正确')
    .nullable(),  // 可选字段
  
  // 年龄范围
  age: number()
    .required('年龄不能为空')
    .min(0, '年龄不能小于 0')
    .max(150, '年龄不能大于 150'),
  
  // 日期
  birthDate: date()
    .required('出生日期不能为空')
    .max(new Date(), '出生日期不能晚于今天'),
});
</script>
```

#### 场景：表单组件使用
- **当** 使用表单组件时
- **那么** 必须：
```vue
<template>
  <Form 
    v-slot="{ errors, isSubmitting, isValid }" 
    :validation-schema="validationSchema"
    @submit="onSubmit"
  >
    <!-- 姓名 -->
    <Field name="patientName" v-slot="{ field, errorMessage, meta }">
      <div :class="{ 'error': errorMessage && meta.touched }">
        <label>患者姓名</label>
        <input 
          v-bind="field" 
          type="text"
          placeholder="请输入患者姓名"
        />
        <span v-if="errorMessage && meta.touched" class="error-text">
          {{ errorMessage }}
        </span>
      </div>
    </Field>
    
    <!-- 身份证号 -->
    <Field name="idCard" v-slot="{ field, errorMessage, meta }">
      <div :class="{ 'error': errorMessage && meta.touched }">
        <label>身份证号</label>
        <input 
          v-bind="field" 
          type="text"
          placeholder="请输入身份证号"
        />
        <span v-if="errorMessage && meta.touched" class="error-text">
          {{ errorMessage }}
        </span>
      </div>
    </Field>
    
    <!-- 提交按钮 -->
    <button 
      type="submit" 
      :disabled="isSubmitting || !isValid"
    >
      {{ isSubmitting ? '保存中...' : '保存' }}
    </button>
  </Form>
</template>
```

### 需求：错误提示规范

必须提供清晰、友好的错误提示，支持无障碍访问。

#### 场景：错误提示样式
- **当** 显示错误提示时
- **那么** 必须：
```vue
<style scoped>
.error {
  border-color: #ff4d4f;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

/* 无障碍访问 */
.error-text::before {
  content: "⚠️ ";
}

/* 屏幕阅读器支持 */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
</style>
```

#### 场景：错误提示时机
- **当** 触发错误提示时
- **那么** 必须：
  - 用户输入后不立即显示错误
  - 字段失焦（blur）后显示错误
  - 提交时显示所有错误
  - 用户开始修改后隐藏错误

### 需求：自定义验证规则

必须为医疗特定字段定义自定义验证规则。

#### 场景：医疗字段验证
- **当** 验证医疗字段时
- **那么** 必须：
```typescript
// 医保卡号验证
defineRule('insuranceCard', (value: string) => {
  if (!value) return true;
  // 各地医保卡号格式不同，这里使用通用规则
  return /^[A-Z0-9]{8,20}$/.test(value) || '医保卡号格式不正确';
});

// 病历号验证
defineRule('medicalRecordNumber', (value: string) => {
  if (!value) return true;
  return /^[A-Z]\d{6,10}$/.test(value) || '病历号格式不正确（格式：字母 +6-10 位数字）';
});

// 科室代码验证
defineRule('departmentCode', (value: string) => {
  if (!value) return true;
  const validCodes = ['CARD', 'NEUR', 'PEDI', 'SURG', 'INTERN'];
  return validCodes.includes(value) || '科室代码不正确';
});
```

#### 场景：异步验证
- **当** 需要异步验证时
- **那么** 必须：
```typescript
// 检查身份证号是否已存在
defineRule('uniqueIdCard', async (value: string) => {
  if (!value) return true;
  
  try {
    const response = await api.checkIdCardExists(value);
    return !response.exists || '该身份证号已登记';
  } catch (error) {
    console.error('验证失败:', error);
    return true;  // 验证服务失败时不阻止提交
  }
});
```

### 需求：表单提交规范

必须规范表单提交流程，防止重复提交。

#### 场景：提交处理
- **当** 处理表单提交时
- **那么** 必须：
```vue
<script setup lang="ts">
const onSubmit = async (values: any) => {
  try {
    // isSubmitting 自动设为 true
    await api.savePatient(values);
    
    // 成功提示
    toast.success('保存成功');
    
    // 重置表单
    resetForm();
  } catch (error) {
    // 错误处理
    toast.error('保存失败：' + error.message);
  } finally {
    // isSubmitting 自动恢复
  }
};
</script>
```

#### 场景：防止重复提交
- **当** 防止重复提交时
- **那么** 必须：
  - 提交时禁用提交按钮
  - 显示加载状态（"保存中..."）
  - 使用防抖/节流（如需要）
  - 提交成功后重置表单状态
