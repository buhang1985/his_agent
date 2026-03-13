/**
 * 表单验证工具函数
 * 提供中文验证消息和自定义验证规则
 */

/**
 * 验证手机号（中国大陆）
 */
export function validatePhone(phone: string): { valid: boolean; message?: string } {
  if (!phone) {
    return { valid: false, message: '手机号不能为空' };
  }
  
  const phoneRegex = /^1[3-9]\d{9}$/;
  if (!phoneRegex.test(phone)) {
    return { valid: false, message: '请输入有效的 11 位手机号' };
  }
  
  return { valid: true };
}

/**
 * 验证身份证号（中国大陆）
 */
export function validateIdCard(idCard: string): { valid: boolean; message?: string } {
  if (!idCard) {
    return { valid: false, message: '身份证号不能为空' };
  }
  
  const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/;
  if (!idCardRegex.test(idCard)) {
    return { valid: false, message: '请输入有效的身份证号' };
  }
  
  return { valid: true };
}

/**
 * 验证邮箱
 */
export function validateEmail(email: string): { valid: boolean; message?: string } {
  if (!email) {
    return { valid: false, message: '邮箱不能为空' };
  }
  
  const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(email)) {
    return { valid: false, message: '请输入有效的邮箱地址' };
  }
  
  return { valid: true };
}

/**
 * 验证密码强度
 */
export function validatePassword(password: string): { valid: boolean; message?: string; strength?: 'weak' | 'medium' | 'strong' } {
  if (!password) {
    return { valid: false, message: '密码不能为空' };
  }
  
  if (password.length < 6) {
    return { valid: false, message: '密码长度至少 6 位' };
  }
  
  if (password.length > 20) {
    return { valid: false, message: '密码长度不能超过 20 位' };
  }
  
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasNumber = /\d/.test(password);
  const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
  
  const strengthCount = [hasLetter, hasNumber, hasSpecial].filter(Boolean).length;
  
  let strength: 'weak' | 'medium' | 'strong' = 'weak';
  if (strengthCount >= 3) {
    strength = 'strong';
  } else if (strengthCount >= 2) {
    strength = 'medium';
  }
  
  if (strength === 'weak') {
    return { 
      valid: false, 
      message: '密码必须包含字母和数字，建议包含特殊字符',
      strength 
    };
  }
  
  return { valid: true, strength };
}

/**
 * 验证姓名
 */
export function validateName(name: string): { valid: boolean; message?: string } {
  if (!name) {
    return { valid: false, message: '姓名不能为空' };
  }
  
  if (name.length < 2 || name.length > 20) {
    return { valid: false, message: '姓名长度必须在 2-20 个字符之间' };
  }
  
  const chineseNameRegex = /^[\u4e00-\u9fa5]{2,20}$/;
  if (!chineseNameRegex.test(name)) {
    return { valid: false, message: '姓名只能包含中文字符' };
  }
  
  return { valid: true };
}

/**
 * 验证必填字段
 */
export function validateRequired(value: any, fieldName = '字段'): { valid: boolean; message?: string } {
  if (value === null || value === undefined || value === '') {
    return { valid: false, message: `${fieldName}不能为空` };
  }
  
  if (typeof value === 'string' && value.trim() === '') {
    return { valid: false, message: `${fieldName}不能为空` };
  }
  
  return { valid: true };
}

/**
 * 验证长度范围
 */
export function validateLength(
  value: string, 
  min: number, 
  max: number, 
  fieldName = '字段'
): { valid: boolean; message?: string } {
  if (!value) {
    return { valid: true }; // 空值由 required 验证处理
  }
  
  if (value.length < min) {
    return { valid: false, message: `${fieldName}长度不能少于${min}个字符` };
  }
  
  if (value.length > max) {
    return { valid: false, message: `${fieldName}长度不能超过${max}个字符` };
  }
  
  return { valid: true };
}

/**
 * 验证数字范围
 */
export function validateRange(
  value: number, 
  min: number, 
  max: number, 
  fieldName = '字段'
): { valid: boolean; message?: string } {
  if (value === null || value === undefined) {
    return { valid: true };
  }
  
  if (value < min || value > max) {
    return { valid: false, message: `${fieldName}必须在${min}到${max}之间` };
  }
  
  return { valid: true };
}

/**
 * 组合验证器
 */
export function composeValidators(...validators: Array<(value: any) => { valid: boolean; message?: string }>) {
  return function(value: any): { valid: boolean; message?: string } {
    for (const validator of validators) {
      const result = validator(value);
      if (!result.valid) {
        return result;
      }
    }
    return { valid: true };
  };
}
