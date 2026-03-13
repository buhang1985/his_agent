/**
 * 异步验证器
 * 用于需要调用后端的验证（如用户名唯一性检查）
 */

/**
 * 防抖函数
 */
function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => Promise<ReturnType<T>> {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  
  return function (this: any, ...args: Parameters<T>): Promise<ReturnType<T>> {
    return new Promise((resolve) => {
      if (timeout) {
        clearTimeout(timeout);
      }
      
      timeout = setTimeout(() => {
        resolve(func.apply(this, args));
      }, wait);
    });
  };
}

/**
 * 验证用户名唯一性
 */
export const validateUsernameUnique = debounce(async (username: string): Promise<{ valid: boolean; message?: string }> => {
  if (!username) {
    return { valid: false, message: '用户名不能为空' };
  }
  
  if (username.length < 3 || username.length > 20) {
    return { valid: false, message: '用户名长度必须在 3-20 个字符之间' };
  }
  
  try {
    // TODO: 实际项目中替换为真实 API 调用
    // const response = await http.get(`/api/users/check-username?username=${username}`);
    // const exists = response.data.exists;
    
    // 模拟 API 调用
    await new Promise(resolve => setTimeout(resolve, 500));
    const exists = false; // 模拟不存在
    
    if (exists) {
      return { valid: false, message: '用户名已存在' };
    }
    
    return { valid: true };
  } catch (error) {
    console.error('Username validation failed:', error);
    return { valid: false, message: '验证失败，请稍后重试' };
  }
}, 300);

/**
 * 验证手机号唯一性
 */
export const validatePhoneUnique = debounce(async (phone: string): Promise<{ valid: boolean; message?: string }> => {
  if (!phone) {
    return { valid: false, message: '手机号不能为空' };
  }
  
  const phoneRegex = /^1[3-9]\d{9}$/;
  if (!phoneRegex.test(phone)) {
    return { valid: false, message: '请输入有效的 11 位手机号' };
  }
  
  try {
    // TODO: 实际项目中替换为真实 API 调用
    // const response = await http.get(`/api/users/check-phone?phone=${phone}`);
    // const exists = response.data.exists;
    
    // 模拟 API 调用
    await new Promise(resolve => setTimeout(resolve, 500));
    const exists = false; // 模拟不存在
    
    if (exists) {
      return { valid: false, message: '该手机号已注册' };
    }
    
    return { valid: true };
  } catch (error) {
    console.error('Phone validation failed:', error);
    return { valid: false, message: '验证失败，请稍后重试' };
  }
}, 300);

/**
 * 验证邮箱唯一性
 */
export const validateEmailUnique = debounce(async (email: string): Promise<{ valid: boolean; message?: string }> => {
  if (!email) {
    return { valid: false, message: '邮箱不能为空' };
  }
  
  const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(email)) {
    return { valid: false, message: '请输入有效的邮箱地址' };
  }
  
  try {
    // TODO: 实际项目中替换为真实 API 调用
    // const response = await http.get(`/api/users/check-email?email=${email}`);
    // const exists = response.data.exists;
    
    // 模拟 API 调用
    await new Promise(resolve => setTimeout(resolve, 500));
    const exists = false; // 模拟不存在
    
    if (exists) {
      return { valid: false, message: '该邮箱已注册' };
    }
    
    return { valid: true };
  } catch (error) {
    console.error('Email validation failed:', error);
    return { valid: false, message: '验证失败，请稍后重试' };
  }
}, 300);
