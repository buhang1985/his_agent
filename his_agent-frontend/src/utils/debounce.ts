/**
 * 防抖函数
 * 在 n 秒内只执行最后一次函数
 */
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  
  return function (this: any, ...args: Parameters<T>) {
    if (timeout) {
      clearTimeout(timeout);
    }
    
    timeout = setTimeout(() => {
      func.apply(this, args);
    }, wait);
  };
}

/**
 * 节流函数
 * 在 n 秒内只执行一次函数
 */
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  let previous = 0;
  
  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now();
    const remaining = wait - (now - previous);
    
    if (remaining <= 0) {
      if (timeout) {
        clearTimeout(timeout);
        timeout = null;
      }
      
      previous = now;
      func.apply(this, args);
    } else if (!timeout) {
      timeout = setTimeout(() => {
        previous = Date.now();
        timeout = null;
        func.apply(this, args);
      }, remaining);
    }
  };
}

/**
 * 延迟执行
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 限制并发请求数量
 */
export async function promiseLimit<T>(
  limit: number,
  promises: Array<Promise<T>>
): Promise<Array<T>> {
  const results: Array<T> = [];
  const executing: Array<Promise<any>> = [];
  
  for (const promise of promises) {
    const p = promise.then(result => {
      results.push(result);
      executing.splice(executing.indexOf(p), 1);
      return result;
    });
    
    executing.push(p);
    
    if (executing.length >= limit) {
      await Promise.race(executing);
    }
  }
  
  await Promise.all(executing);
  return results;
}
