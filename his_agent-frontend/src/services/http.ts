/**
 * Axios 统一 HTTP 客户端配置
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { ElMessage } from 'element-plus';

// 响应接口定义
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

// 创建 Axios 实例
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
http.interceptors.request.use(
  (config) => {
    // 添加认证 Token
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // 添加 TraceId（用于链路追踪）
    const traceId = generateTraceId();
    config.headers['X-Trace-Id'] = traceId;

    // 添加请求时间戳（防止缓存）
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now(),
      };
    }

    return config;
  },
  (error: AxiosError) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data;

    // 如果响应码不是 200，处理错误
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败');
      
      // 401: 未授权，跳转到登录页
      if (res.code === 401) {
        localStorage.removeItem('auth_token');
        window.location.href = '/login';
      }
      
      // 403: 禁止访问
      if (res.code === 403) {
        ElMessage.error('没有访问权限');
      }

      return Promise.reject(new Error(res.message || '请求失败'));
    }

    return response;
  },
  (error: AxiosError<ApiResponse>) => {
    console.error('Response error:', error);

    // 网络错误
    if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络');
      return Promise.reject(error);
    }

    const status = error.response.status;
    const message = error.response.data?.message || getErrorMessage(status);

    ElMessage.error(message);

    // 特殊状态码处理
    if (status === 401) {
      localStorage.removeItem('auth_token');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

// 生成 TraceId
function generateTraceId(): string {
  const timestamp = Date.now().toString(36);
  const randomPart = Math.random().toString(36).substring(2, 9);
  return `${timestamp}-${randomPart}`;
}

// 获取错误消息
function getErrorMessage(status: number): string {
  const errorMessages: Record<number, string> = {
    400: '请求参数错误',
    401: '未授权，请重新登录',
    403: '没有访问权限',
    404: '请求的资源不存在',
    408: '请求超时',
    500: '服务器内部错误',
    502: '网关错误',
    503: '服务不可用',
    504: '网关超时',
  };
  return errorMessages[status] || `请求失败 (${status})`;
}

// 封装请求方法
export const httpService = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return http.get<T>(url, config);
  },

  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return http.post<T>(url, data, config);
  },

  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return http.put<T>(url, data, config);
  },

  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<ApiResponse<T>>> {
    return http.delete<T>(url, config);
  },

  upload<T = any>(url: string, formData: FormData): Promise<AxiosResponse<ApiResponse<T>>> {
    return http.post<T>(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};

export default http;
