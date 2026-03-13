# 前端性能规范

**版本**: 1.0  
**日期**: 2026-03-12  
**状态**: 新增

---

## 新增需求

### 需求：首屏加载时间约束

首屏加载时间必须达到性能指标要求，提升用户体验。

#### 场景：性能指标要求
- **当** 测量首屏加载时
- **那么** 必须满足：
```yaml
# 性能指标（4G 网络）
performance:
  fcp (First Contentful Paint): ≤ 1.5s
  lcp (Largest Contentful Paint): ≤ 2.5s
  fid (First Input Delay): ≤ 100ms
  cls (Cumulative Layout Shift): ≤ 0.1
  tti (Time to Interactive): ≤ 3.5s
  
# 网络要求
network:
  首屏资源大小：≤ 500KB（gzip 后）
  JS bundle 大小：≤ 300KB（gzip 后）
  CSS 大小：≤ 50KB（gzip 后）
  图片资源：≤ 150KB（单张）
```

#### 场景：性能监控
- **当** 监控性能时
- **那么** 必须：
```typescript
// src/utils/performance.ts
export function reportWebVitals() {
  import('web-vitals').then(({ getCLS, getFID, getFCP, getLCP, getTTFB }) => {
    getCLS(console.log);
    getFID(console.log);
    getFCP(console.log);
    getLCP((metric) => {
      console.log('LCP:', metric.value);
      // 上报到监控系统
      reportToAnalytics(metric);
    });
    getTTFB(console.log);
  });
}
```

---

### 需求：路由懒加载规范

路由必须实现懒加载，减少初始 bundle 大小。

#### 场景：路由懒加载
- **当** 配置路由时
- **那么** 必须：
```typescript
// src/router.ts
import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      // ✅ 正确：动态 import 懒加载
      component: () => import('./views/HomeView.vue'),
    },
    {
      path: '/consultation',
      name: 'consultation',
      // ✅ 正确：命名 chunk
      component: () => import(/* webpackChunkName: "consultation" */ './views/ConsultationView.vue'),
    },
    {
      path: '/__dev',
      name: 'DevPortal',
      // ✅ 正确：动态 import
      component: () => import('./views/DevPortal.vue'),
    },
  ],
});

export default router;
```

#### 场景：组件懒加载
- **当** 加载大组件时
- **那么** 必须：
```vue
<!-- ✅ 正确：defineAsyncComponent 懒加载 -->
<script setup lang="ts">
import { defineAsyncComponent } from 'vue';

const LargeChart = defineAsyncComponent(() => import('./components/LargeChart.vue'));
const RichTextEditor = defineAsyncComponent(() => import('./components/RichTextEditor.vue'));
</script>
```

#### 场景：路由守卫
- **当** 配置路由守卫时
- **那么** 必须：
```typescript
// src/router.ts
router.beforeEach((to, from, next) => {
  // 进度条开始
  NProgress.start();
  
  // 认证检查
  const token = useAuthStore().accessToken;
  if (to.meta.requiresAuth && !token) {
    next({ name: 'login', query: { redirect: to.fullPath } });
    return;
  }
  
  next();
});

router.afterEach(() => {
  NProgress.done();
});
```

---

### 需求：静态资源缓存策略

静态资源必须实现合理的缓存策略，减少重复请求。

#### 场景：Vite 构建配置
- **当** 配置 Vite 时
- **那么** 必须：
```typescript
// vite.config.ts
export default defineConfig({
  build: {
    target: 'esnext',
    minify: 'esbuild',
    sourcemap: false,  // 生产环境关闭 source map
    
    // 代码分割
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus'],
          'utils': ['./src/utils'],
        },
      },
    },
    
    // 资源输出目录
    assetsDir: 'assets',
    
    // 资源大小限制
    assetsInlineLimit: 4096,  // 4KB 以下内联为 base64
  },
  
  // 预加载
  build: {
    rollupOptions: {
      plugins: [
        // 预加载关键资源
      ],
    },
  },
});
```

#### 场景：Nginx 缓存配置
- **当** 配置 Nginx 时
- **那么** 必须：
```nginx
# /etc/nginx/nginx.conf
http {
    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied expired no-cache no-store private auth;
    gzip_types text/plain text/css text/xml text/javascript 
               application/x-javascript application/xml application/javascript 
               application/json;
    gzip_disable "MSIE [1-6]\.";
    
    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        add_header X-Cache-Status "HIT";
        
        # 版本控制文件（带 hash）
        if ($uri ~* "^/assets/.*\.[a-f0-9]{8}\.") {
            expires 1y;
            add_header Cache-Control "public, immutable, max-age=31536000";
        }
    }
    
    # HTML 不缓存
    location ~* \.html$ {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://his_agent_backend;
        proxy_cache_bypass $http_pragma $http_authorization;
        proxy_no_cache $http_pragma $http_authorization;
    }
}
```

#### 场景：浏览器缓存策略
- **当** 配置响应头时
- **那么** 必须：
```yaml
# 缓存策略
cache_control:
  # HTML 文件（带版本号）
  index.html: no-cache
  
  # JS/CSS（带 hash）
  assets/*.abc123.js: public, immutable, max-age=31536000
  assets/*.def456.css: public, immutable, max-age=31536000
  
  # 图片资源
  assets/*.png: public, max-age=2592000  # 30 天
  assets/*.jpg: public, max-age=2592000
  
  # 字体
  assets/*.woff2: public, max-age=31536000
```

---

### 需求：图片优化

图片资源必须优化，减少加载时间。

#### 场景：图片格式选择
- **当** 使用图片时
- **那么** 必须：
  - 优先使用 WebP 格式（体积小 30%）
  - 提供 fallback（兼容不支持 WebP 的浏览器）
  - 图标使用 SVG
  - 动画使用 Lottie（替代 GIF）

#### 场景：图片懒加载
- **当** 加载图片时
- **那么** 必须：
```vue
<!-- ✅ 正确：原生懒加载 -->
<img src="image.jpg" loading="lazy" alt="description" />

<!-- ✅ 正确：Element Plus 懒加载 -->
<el-image 
  src="image.jpg" 
  :lazy="true"
  placeholder="placeholder.jpg"
/>
```

#### 场景：响应式图片
- **当** 展示图片时
- **那么** 必须：
```html
<!-- 响应式图片 -->
<img
  srcset="
    image-320w.jpg 320w,
    image-480w.jpg 480w,
    image-800w.jpg 800w
  "
  sizes="(max-width: 320px) 280px, (max-width: 480px) 440px, 800px"
  src="image-800w.jpg"
  alt="description"
/>
```

---

### 需求：Tree Shaking

必须启用 Tree Shaking，移除未使用代码。

#### 场景：ESM 模块
- **当** 导入库时
- **那么** 必须：
```typescript
// ✅ 正确：按需导入
import { Button, Dialog } from 'element-plus';
import { ref, computed } from 'vue';

// ❌ 错误：全量导入（禁止）
import * as ElementPlus from 'element-plus';
import * as Vue from 'vue';
```

#### 场景：工具函数
- **当** 编写工具函数时
- **那么** 必须：
```typescript
// ✅ 正确：命名导出
export function formatDate(date: Date): string { ... }
export function formatMoney(amount: number): string { ... }

// ❌ 错误：默认导出整个对象（影响 Tree Shaking）
export default {
  formatDate,
  formatMoney,
};
```

---

### 需求：虚拟滚动

长列表必须使用虚拟滚动，减少 DOM 节点。

#### 场景：虚拟滚动实现
- **当** 展示长列表时
- **那么** 必须：
```vue
<!-- ✅ 正确：Element Plus 虚拟列表 -->
<template>
  <el-table-v2
    :columns="columns"
    :data="largeData"
    :width="700"
    :height="400"
  />
</template>

<!-- ✅ 正确：vue-virtual-scroller -->
<template>
  <RecycleScroller
    :items="largeList"
    :item-size="50"
    key-field="id"
  >
    <template #default="{ item }">
      <div class="item">{{ item.name }}</div>
    </template>
  </RecycleScroller>
</template>
```

---

### 需求：防抖节流

频繁触发的事件必须使用防抖或节流。

#### 场景：防抖实现
- **当** 处理频繁事件时
- **那么** 必须：
```typescript
// src/composables/useDebounce.ts
import { ref } from 'vue';

export function useDebounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
) {
  let timer: ReturnType<typeof setTimeout> | null = null;
  
  return (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

// 使用示例
const search = useDebounce((keyword: string) => {
  api.search(keyword);
}, 300);
```

#### 场景：节流实现
- **当** 处理滚动/窗口事件时
- **那么** 必须：
```typescript
// src/composables/useThrottle.ts
export function useThrottle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 100
) {
  let lastTime = 0;
  
  return (...args: Parameters<T>) => {
    const now = Date.now();
    if (now - lastTime >= delay) {
      lastTime = now;
      fn(...args);
    }
  };
}

// 使用示例
const handleScroll = useThrottle(() => {
  loadMore();
}, 200);

window.addEventListener('scroll', handleScroll);
```

---

## 验收标准

### 首屏加载
- [ ] FCP ≤ 1.5s
- [ ] LCP ≤ 2.5s
- [ ] 首屏资源 ≤ 500KB（gzip）
- [ ] JS bundle ≤ 300KB（gzip）

### 路由懒加载
- [ ] 所有路由使用动态 import
- [ ] 大组件使用 defineAsyncComponent
- [ ] 路由守卫配置正确

### 静态资源缓存
- [ ] Vite 代码分割配置
- [ ] Nginx 缓存策略配置
- [ ] Gzip 压缩开启
- [ ] HTML 不缓存，静态资源长缓存

### 图片优化
- [ ] 优先使用 WebP
- [ ] 图片懒加载实现
- [ ] 响应式图片支持

### Tree Shaking
- [ ] 按需导入库
- [ ] 使用命名导出
- [ ] 无全量导入

### 虚拟滚动
- [ ] 长列表使用虚拟滚动
- [ ] 单页 DOM 节点 ≤ 1500

### 防抖节流
- [ ] 搜索框防抖实现
- [ ] 滚动事件节流实现
- [ ] 窗口大小事件节流实现
