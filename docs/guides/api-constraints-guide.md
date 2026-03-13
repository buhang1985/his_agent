# API 约束使用文档

**版本**: 1.0  
**日期**: 2026-03-12  
**适用**: 前后端开发人员

---

## 1. 统一 API 响应格式

### 1.1 响应结构

所有 API 响应遵循统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "traceId": "t1234567890-abc123"
}
```

### 1.2 错误码规范

错误码格式：`XXXYYY`
- `XXX`: 模块标识（100=通用，200=用户，300=患者，400=问诊，500=语音，600=LLM，700=HIS）
- `YYY`: 具体错误

```java
public enum ErrorCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    
    // 用户相关 200xxx
    USER_NOT_FOUND(200001, "用户不存在"),
    
    // 患者相关 300xxx
    PATIENT_NOT_FOUND(300001, "患者不存在"),
    
    // 问诊相关 400xxx
    CONSULTATION_NOT_FOUND(400001, "问诊会话不存在"),
}
```

### 1.3 分页响应

```java
PageResponse<T> response = PageResponse.of(
    content,      // 数据列表
    page,         // 当前页码（从 0 开始）
    size,         // 每页大小
    totalElements // 总记录数
);
```

响应示例：
```json
{
  "code": 200,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

---

## 2. 数据脱敏规范

### 2.1 脱敏工具使用

```java
import com.hisagent.util.DataMaskingUtils;

// 手机号脱敏：13812345678 -> 138****5678
String maskedPhone = DataMaskingUtils.maskPhone("13812345678");

// 身份证脱敏：110101199001011234 -> 110101********1234
String maskedIdCard = DataMaskingUtils.maskIdCard("110101199001011234");

// 姓名脱敏：张三 -> 张*
String maskedName = DataMaskingUtils.maskName("张三");

// 邮箱脱敏：test@example.com -> t***t@example.com
String maskedEmail = DataMaskingUtils.maskEmail("test@example.com");

// 地址脱敏：北京市海淀区中关村大街 1 号 -> 北京市海淀区****
String maskedAddress = DataMaskingUtils.maskAddress("北京市海淀区中关村大街 1 号");

// 自定义脱敏：保留前后缀
String masked = DataMaskingUtils.maskPartial("abcdef123456", 3, 3);
// 结果：abc********456
```

### 2.2 使用 @MaskData 注解

```java
import com.hisagent.annotation.MaskData;
import com.hisagent.annotation.MaskType;

@Data
public class PatientDTO {
    
    @MaskData(type = MaskType.NAME)
    private String name;
    
    @MaskData(type = MaskType.PHONE)
    private String phone;
    
    @MaskData(type = MaskType.ID_CARD)
    private String idCard;
    
    @MaskData(type = MaskType.EMAIL)
    private String email;
}
```

### 2.3 脱敏规则说明

| 类型 | 规则 | 示例 |
|------|------|------|
| 手机号 | 保留前 3 后 4 | 138****5678 |
| 身份证 | 保留前 6 后 4 | 110101********1234 |
| 姓名 | 单字*，双字首*，三字首尾* | 张*，张*丰，欧阳*华 |
| 邮箱 | 保留首尾字符 | t***t@example.com |
| 地址 | 保留前 6 字符 | 北京市海淀区**** |

---

## 3. 分页参数规范

### 3.1 请求参数

```
GET /api/patients?page=0&size=20&sort=name,asc
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 0 | 页码（从 0 开始） |
| size | int | 20 | 每页大小（1-100） |
| sort | string | - | 排序字段，格式：field,asc/desc |

### 3.2 参数验证

- `page` 必须 >= 0
- `size` 必须在 1-100 之间
- 超出范围返回 400 错误

---

## 4. TraceId 链路追踪

### 4.1 请求头传递

前端自动添加 `X-Trace-Id` 请求头：

```typescript
// 前端自动添加
headers: {
  'X-Trace-Id': 't1234567890-abc123'
}
```

### 4.2 日志输出

所有日志自动包含 TraceId：

```
2026-03-12 10:30:00.123 [his-agent] [http-nio-8080-exec-1] INFO  c.h.c.PatientController - [t1234567890-abc123] Get patient 123
```

### 4.3 响应头返回

响应自动包含 `X-Trace-Id` 头，便于问题排查。

---

## 5. 最佳实践

### 5.1 Controller 层

```java
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {
    
    private final PatientService patientService;
    
    @GetMapping("/{id}")
    public ApiResponse<PatientDTO> getPatient(@PathVariable String id) {
        PatientDTO patient = patientService.getPatient(id);
        return ApiResponse.success(patient);
    }
    
    @PostMapping
    public ApiResponse<PatientDTO> createPatient(@Valid @RequestBody CreatePatientRequest request) {
        PatientDTO patient = patientService.createPatient(request);
        return ApiResponse.success(patient);
    }
}
```

### 5.2 Service 层

```java
@Service
@RequiredArgsConstructor
public class PatientService {
    
    private final PatientRepository patientRepository;
    
    public PatientDTO getPatient(String id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));
        
        return convertToDTO(patient);
    }
}
```

### 5.3 异常处理

```java
try {
    patientService.deletePatient(id);
} catch (BusinessException e) {
    log.error("Delete patient failed", e);
    throw e; // 全局异常处理器会统一处理
}
```

---

## 6. 相关文档

- [数据脱敏使用文档](./data-masking-guide.md)
- [监控告警配置文档](./monitoring-guide.md)
- [熔断降级配置文档](./circuit-breaker-guide.md)
