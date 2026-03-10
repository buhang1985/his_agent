# 前端测试规范

## 测试框架

- **单元测试**: Vitest (Vite 原生测试框架)
- **组件测试**: @vue/test-utils
- **Mock 工具**: Vitest 内置 mock + MSW (API Mock)
- **测试覆盖率**: Vitest Coverage (要求 ≥ 80%)

## 测试分类

### 1. 单元测试 (`*.test.ts`)

测试工具函数、Composables、Services。

**示例**:
```typescript
// src/utils/__tests__/medicalTermCorrector.test.ts
import { describe, it, expect } from 'vitest';
import { MedicalTermCorrector } from '../medicalTermCorrector';

describe('MedicalTermCorrector', () => {
  const corrector = new MedicalTermCorrector();
  
  it('should correct common medical term', () => {
    const input = '高血鸭';
    const result = corrector.correct(input);
    expect(result).toBe('高血压');
  });
  
  it('should preserve correct term', () => {
    const input = '高血压';
    const result = corrector.correct(input);
    expect(result).toBe('高血压');
  });
});
```

### 2. 组件测试 (`*.spec.ts`)

测试 Vue 组件的渲染和交互。

**示例**:
```typescript
// src/components/__tests__/VoiceInput.spec.ts
import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import VoiceInput from '../VoiceInput.vue';

describe('VoiceInput', () => {
  it('should render start button when not listening', () => {
    const wrapper = mount(VoiceInput, {
      props: {
        isListening: false,
        isProcessing: false,
      },
    });
    
    expect(wrapper.text()).toContain('🎤 开始问诊');
  });
  
  it('should emit start event when button clicked', async () => {
    const wrapper = mount(VoiceInput, {
      props: {
        isListening: false,
        isProcessing: false,
      },
    });
    
    await wrapper.find('button').trigger('click');
    expect(wrapper.emitted('start')).toBeDefined();
    expect(wrapper.emitted('start')).toHaveLength(1);
  });
  
  it('should show recording indicator when listening', () => {
    const wrapper = mount(VoiceInput, {
      props: {
        isListening: true,
        isProcessing: false,
      },
    });
    
    expect(wrapper.find('.voice-input__indicator').exists()).toBe(true);
  });
});
```

### 3. Composable 测试

测试 Vue 3 Composition API 逻辑。

**示例**:
```typescript
// src/composables/__tests__/useVoiceConsultation.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useVoiceConsultation } from '../useVoiceConsultation';

describe('useVoiceConsultation', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });
  
  it('should initialize with idle status', () => {
    const { status } = useVoiceConsultation();
    expect(status.value).toBe('idle');
  });
  
  it('should update status to recording when start', async () => {
    const { status, startConsultation } = useVoiceConsultation();
    
    await startConsultation('P001');
    expect(status.value).toBe('recording');
  });
});
```

## 测试执行流程

### 串行执行要求

**重要**: 每个任务必须等待上一个任务测试通过后才能执行。

```bash
# 1. 安装依赖
npm install

# 2. 运行单元测试
npm run test

# 3. 运行带 UI 的测试 (可选)
npm run test:ui

# 4. 生成覆盖率报告
npm run test:coverage

# 5. 类型检查
npm run typecheck

# 6. 代码检查
npm run lint
```

### 任务执行检查清单

在执行下一个任务前，必须确认：

- [ ] 当前任务所有单元测试通过
- [ ] 当前任务所有组件测试通过
- [ ] 测试覆盖率 ≥ 80%
- [ ] TypeScript 类型检查通过
- [ ] ESLint 检查通过
- [ ] 代码已格式化 (Prettier)

### CI/CD 集成

```yaml
# GitHub Actions 示例
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run tests
        run: npm run test -- --run
      
      - name: Check coverage
        run: npm run test:coverage -- --run
      
      - name: Type check
        run: npm run typecheck
      
      - name: Lint
        run: npm run lint
      
      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          files: coverage/coverage.json
```

## 测试最佳实践

### 1. 测试文件命名

```
src/
├── components/
│   └── VoiceInput.vue
│   └── __tests__/
│       └── VoiceInput.spec.ts
├── utils/
│   └── medicalTermCorrector.ts
│   └── __tests__/
│       └── medicalTermCorrector.test.ts
└── composables/
    └── useVoiceConsultation.ts
    └── __tests__/
        └── useVoiceConsultation.test.ts
```

### 2. 测试描述规范

```typescript
describe('ComponentName', () => {
  it('should do something when condition', () => {
    // ...
  });
  
  it('should handle edge case', () => {
    // ...
  });
  
  describe('Nested describe for complex components', () => {
    it('should...', () => { });
  });
});
```

### 3. Arrange-Act-Assert 模式

```typescript
it('should calculate total price correctly', () => {
  // Arrange
  const items = [
    { price: 100, quantity: 2 },
    { price: 50, quantity: 1 },
  ];
  
  // Act
  const total = calculateTotal(items);
  
  // Assert
  expect(total).toBe(250);
});
```

### 4. Mock API 请求

```typescript
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  http.post('/api/v1/consultations', () => {
    return HttpResponse.json({ id: '1', patientId: 'P001' });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

## 覆盖率要求

| 模块 | 行覆盖率 | 分支覆盖率 |
|------|----------|------------|
| Composables | ≥ 85% | ≥ 80% |
| Services | ≥ 85% | ≥ 80% |
| Utils | ≥ 90% | ≥ 85% |
| Components | ≥ 75% | ≥ 70% |
| Views | ≥ 70% | ≥ 65% |

## 常见问题

### Q: 如何调试失败的测试？

```bash
# 运行单个测试文件
npm run test -- src/utils/__tests__/medicalTermCorrector.test.ts

# 运行匹配的测试
npm run test -- -t "should correct medical term"

# 监听模式 (文件变化自动重跑)
npm run test -- --watch
```

### Q: 如何跳过测试？

```typescript
// 跳过单个测试
it.skip('should skip this test', () => { });

// 只运行某个测试
it.only('should only run this test', () => { });

// 跳过整个 describe
describe.skip('Skip this suite', () => { });
```

### Q: 如何测试异步代码？

```typescript
it('should fetch data asynchronously', async () => {
  const data = await fetchData();
  expect(data).toBeDefined();
});

it('should handle async error', async () => {
  await expect(failingFunction()).rejects.toThrow('Error message');
});
```

## 参考资料

- [Vitest 文档](https://vitest.dev/)
- [Vue Test Utils 文档](https://test-utils.vuejs.org/)
- [Testing Library](https://testing-library.com/)
