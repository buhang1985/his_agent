# 数据脱敏使用文档

**版本**: 1.0  
**日期**: 2026-03-12  
**适用**: 前后端开发人员、数据安全管理员

---

## 1. 概述

本系统提供完善的数据脱敏机制，用于保护患者隐私数据，符合医疗数据安全管理要求。

### 1.1 脱敏场景

- **展示脱敏**: 前端展示时脱敏（后端返回已脱敏数据）
- **日志脱敏**: 日志输出时自动脱敏
- **接口脱敏**: API 响应数据脱敏

---

## 2. 后端脱敏

### 2.1 工具类脱敏

```java
import com.hisagent.util.DataMaskingUtils;

@Service
public class PatientService {
    
    public PatientDTO getPatient(String id) {
        Patient patient = patientRepository.findById(id).orElseThrow();
        
        PatientDTO dto = convertToDTO(patient);
        
        // 脱敏处理
        dto.setName(DataMaskingUtils.maskName(dto.getName()));
        dto.setPhone(DataMaskingUtils.maskPhone(dto.getPhone()));
        dto.setIdCard(DataMaskingUtils.maskIdCard(dto.getIdCard()));
        
        return dto;
    }
}
```

### 2.2 注解脱敏

```java
import com.hisagent.annotation.MaskData;
import com.hisagent.annotation.MaskType;

@Data
public class PatientDTO {
    
    private String id;
    
    @MaskData(type = MaskType.NAME)
    private String name;
    
    @MaskData(type = MaskType.PHONE)
    private String phone;
    
    @MaskData(type = MaskType.ID_CARD)
    private String idCard;
    
    @MaskData(type = MaskType.ADDRESS)
    private String address;
}
```

### 2.3 自定义脱敏规则

```java
// 保留前 3 位和后 4 位
String masked = DataMaskingUtils.maskPartial("1234567890", 3, 4);
// 结果：123***7890
```

---

## 3. 前端脱敏

### 3.1 展示脱敏组件

```vue
<template>
  <el-form :model="patient">
    <el-form-item label="姓名">
      <span>{{ maskName(patient.name) }}</span>
    </el-form-item>
    
    <el-form-item label="手机号">
      <span>{{ maskPhone(patient.phone) }}</span>
    </el-form-item>
    
    <el-form-item label="身份证号">
      <span>{{ maskIdCard(patient.idCard) }}</span>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { maskName, maskPhone, maskIdCard } from '@/utils/masking';

const patient = ref({
  name: '张三',
  phone: '13800138000',
  idCard: '110101199001011234'
});
</script>
```

### 3.2 脱敏工具函数

```typescript
// utils/masking.ts

export function maskPhone(phone: string): string {
  if (!phone || phone.length !== 11) return phone;
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
}

export function maskIdCard(idCard: string): string {
  if (!idCard || idCard.length !== 18) return idCard;
  return idCard.replace(/(\d{6})\d{8}(\w{4})/, '$1********$2');
}

export function maskName(name: string): string {
  if (!name) return name;
  if (name.length === 1) return '*';
  if (name.length === 2) return name[0] + '*';
  return name[0] + '*' + name[name.length - 1];
}
```

---

## 4. 日志脱敏

### 4.1 自动脱敏配置

系统已配置敏感数据过滤器，日志中的敏感数据自动脱敏：

```xml
<!-- logback-spring.xml -->
<turboFilter class="com.hisagent.filter.SensitiveDataFilter">
  <OnMatch>DENY</OnMatch>
</turboFilter>
```

### 4.2 日志输出规范

```java
// 正确：直接打印对象，日志框架自动脱敏
log.info("Patient info: {}", patient);

// 错误：手动拼接可能导致脱敏失效
log.info("Patient phone: " + patient.getPhone()); // 可能泄露完整手机号
```

---

## 5. 脱敏规则详解

### 5.1 手机号脱敏

| 原始数据 | 脱敏后 | 规则 |
|----------|--------|------|
| 13812345678 | 138****5678 | 保留前 3 后 4 |
| 19999999999 | 199****9999 | 保留前 3 后 4 |

### 5.2 身份证号脱敏

| 原始数据 | 脱敏后 | 规则 |
|----------|--------|------|
| 110101199001011234 | 110101********1234 | 保留前 6 后 4 |
| 44030119950101567X | 440301********567X | 保留前 6 后 4 |

### 5.3 姓名脱敏

| 原始数据 | 脱敏后 | 规则 |
|----------|--------|------|
| 张 | * | 单字全部脱敏 |
| 张三 | 张* | 双字保留首字 |
| 张三丰 | 张*丰 | 三字保留首尾 |
| 欧阳建华 | 欧阳*华 | 复姓保留前两字和最后一字 |

### 5.4 邮箱脱敏

| 原始数据 | 脱敏后 | 规则 |
|----------|--------|------|
| test@example.com | t***t@example.com | 保留用户名首尾字符 |
| ab@gmail.com | a***b@gmail.com | 保留用户名首尾字符 |
| a@qq.com | a*@qq.com | 单字符保留首字符 |

### 5.5 地址脱敏

| 原始数据 | 脱敏后 | 规则 |
|----------|--------|------|
| 北京市海淀区中关村大街 1 号 | 北京市海淀区**** | 保留前 6 字符 |
| 广东省深圳市南山区 | 广东省深圳市**** | 保留前 6 字符 |
| 北京 | 北*** | 不足 6 字符保留 2 字符 |

---

## 6. 安全注意事项

### 6.1 禁止行为

- ❌ 禁止在日志中打印完整敏感数据
- ❌ 禁止在前端明文展示敏感数据
- ❌ 禁止将敏感数据传递给第三方（除非授权）
- ❌ 禁止关闭脱敏功能

### 6.2 授权访问

只有以下角色可以访问完整敏感数据：

- 主治医生（查看自己患者的完整信息）
- 管理员（审计需要）
- 系统运维（故障排查需要）

### 6.3 审计日志

所有敏感数据访问都会被记录：

```java
@AuditLog(action = "VIEW_PATIENT_INFO", accessLevel = "SENSITIVE")
public PatientDTO getPatient(String id) {
    return patientService.getPatient(id);
}
```

---

## 7. 相关文档

- [API 约束使用文档](./api-constraints-guide.md)
- [安全规范使用文档](./security-guide.md)
