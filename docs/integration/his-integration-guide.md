# HIS 集成联调指南

**版本**: 1.0  
**日期**: 2026-03-12  
**适用**: 实施工程师、集成工程师

---

## 1. 概述

本文档指导实施工程师完成与医院 HIS 系统的集成联调工作。

### 1.1 集成架构

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  HIS Agent 系统 │────▶│  HIS 适配器层    │────▶│   医院 HIS 系统   │
│                 │     │                  │     │                 │
│  - 语音问诊     │     │  - REST 适配器    │     │  - 东软 HIS     │
│  - 病历生成     │     │  - SOAP 适配器    │     │  - 卫宁 HIS     │
│  - 患者管理     │     │  - 定制适配器    │     │  - 创业 HIS     │
└─────────────────┘     └──────────────────┘     └─────────────────┘
```

### 1.2 适配器说明

| 适配器 | 适用场景 | 协议 |
|--------|----------|------|
| RestHisAdapter | 提供 REST API 的 HIS | HTTP/JSON |
| SoapHisAdapter | 提供 SOAP Web Service 的 HIS | SOAP/XML |
| CustomHisAdapter | 需要定制开发的 HIS | 定制 |
| MockHisAdapter | 开发测试环境 | Mock |

---

## 2. 联调前准备

### 2.1 环境要求

- [ ] HIS Agent 系统部署完成
- [ ] 网络连接正常（服务器到 HIS 服务器）
- [ ] 防火墙规则已配置
- [ ] HIS 系统接口文档已获取
- [ ] HIS 测试环境账号已准备

### 2.2 网络连通性测试

```bash
# 测试 HIS 服务器连通性
ping <his-server-ip>

# 测试 HIS 服务端口
telnet <his-server-ip> <port>

# 测试 HTTPS 连接
curl -v https://<his-server-ip>:<port>/health
```

### 2.3 配置 HIS 连接信息

编辑 `application.yml`：

```yaml
app:
  his-service:
    # REST 方式
    rest:
      base-url: https://his.hospital.com/api
      username: his_agent
      password: <password>
    # SOAP 方式
    soap:
      wsdl-url: https://his.hospital.com/his-service?wsdl
      username: his_agent
      password: <password>
    # 超时配置
    connect-timeout: 5000
    read-timeout: 15000
```

---

## 3. 联调测试步骤

### 3.1 第一步：基础连接测试

**目标**: 验证与 HIS 系统的基础连接

```bash
# 使用 curl 测试 HIS 接口
curl -X GET "https://his.hospital.com/api/patients/MOCK_001" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json"
```

**预期结果**:
- HTTP 200 响应
- 返回有效 JSON 数据

**常见问题**:
- 401 未授权：检查认证配置
- 403 禁止访问：检查账号权限
- 404 未找到：检查接口 URL
- 500 服务器错误：联系 HIS 厂商

### 3.2 第二步：患者信息查询测试

**测试用例**:

| 测试项 | 输入 | 预期结果 |
|--------|------|----------|
| 按 ID 查询 | 患者 ID | 返回患者详情 |
| 按身份证查询 | 身份证号 | 返回患者详情 |
| 搜索患者 | 姓名关键词 | 返回患者列表 |
| 查询不存在患者 | 无效 ID | 返回空或 404 |

**Java 测试代码**:

```java
@Autowired
private HisAdapterFactory hisAdapterFactory;

@Test
void testPatientQuery() {
    HisAdapter adapter = hisAdapterFactory.getAdapter("REST");
    
    // 测试按 ID 查询
    HisPatientDTO patient = adapter.getPatient("P123456");
    assertNotNull(patient);
    assertNotNull(patient.getId());
    assertNotNull(patient.getName());
    
    // 测试按身份证查询
    HisPatientDTO patientByIdCard = adapter.getPatientByIdCard("110101199001011234");
    assertNotNull(patientByIdCard);
    
    // 测试搜索
    List<HisPatientDTO> patients = adapter.searchPatients("张");
    assertFalse(patients.isEmpty());
}
```

### 3.3 第三步：问诊数据同步测试

**测试用例**:

| 测试项 | 输入 | 预期结果 |
|--------|------|----------|
| 创建问诊 | 患者 ID+主诉 | 返回问诊 ID |
| 更新问诊 | 问诊 ID+诊断 | 更新成功 |
| 查询问诊 | 问诊 ID | 返回问诊详情 |
| 查询患者问诊历史 | 患者 ID | 返回问诊列表 |

**Java 测试代码**:

```java
@Test
void testConsultationSync() {
    HisAdapter adapter = hisAdapterFactory.getAdapter("REST");
    
    // 创建问诊
    HisConsultationDTO consultation = HisConsultationDTO.builder()
        .patientId("P123456")
        .doctorId("D789")
        .chiefComplaint("头痛，发热")
        .build();
    
    HisConsultationDTO result = adapter.createConsultation(consultation);
    assertNotNull(result.getId());
    
    // 查询问诊
    HisConsultationDTO queried = adapter.getConsultation(result.getId());
    assertEquals("头痛，发热", queried.getChiefComplaint());
}
```

### 3.4 第四步：数据一致性测试

**测试步骤**:

1. 在 HIS 系统创建测试患者
2. 在 HIS Agent 查询该患者
3. 对比两边数据是否一致
4. 修改 HIS 患者信息
5. 再次查询，验证同步更新

**数据对比清单**:

- [ ] 患者姓名一致
- [ ] 身份证号一致
- [ ] 手机号一致
- [ ] 性别一致
- [ ] 年龄一致
- [ ] 地址一致

### 3.5 第五步：异常场景测试

**测试场景**:

| 场景 | 模拟方法 | 预期处理 |
|------|----------|----------|
| 网络断开 | 禁用网卡 | 熔断器打开，返回降级结果 |
| HIS 服务不可用 | 停止 HIS 服务 | 熔断器打开，重试 3 次 |
| 超时 | 设置超时参数 | 超时后返回降级结果 |
| 数据格式错误 | 返回非法 JSON | 抛出异常，记录日志 |

**Java 测试代码**:

```java
@Test
void testExceptionHandling() {
    HisAdapter adapter = hisAdapterFactory.getAdapter("REST");
    
    // 测试不存在患者
    assertThrows(RuntimeException.class, () -> {
        adapter.getPatient("INVALID_ID");
    });
    
    // 测试超时
    assertThrows(RuntimeException.class, () -> {
        adapter.getPatient("TIMEOUT_TEST");
    });
}
```

### 3.6 第六步：性能测试

**测试指标**:

| 指标 | 目标值 | 测试方法 |
|------|--------|----------|
| 单次查询响应时间 | < 2 秒 | JMeter 压测 |
| 并发查询 QPS | > 50 | JMeter 并发 |
| 批量查询性能 | < 5 秒/100 条 | 批量接口测试 |

**JMeter 测试计划**:

1. 创建线程组（50 线程）
2. 添加 HTTP 请求（患者查询接口）
3. 添加聚合报告
4. 运行测试，分析结果

---

## 4. 联调问题排查

### 4.1 常见问题及解决方案

**问题 1: 连接超时**
```
症状：Connection timeout
原因：网络不通或 HIS 服务未启动
解决：
1. 检查网络连接
2. 确认 HIS 服务状态
3. 检查防火墙规则
```

**问题 2: 认证失败**
```
症状：401 Unauthorized
原因：账号密码错误或 Token 过期
解决：
1. 验证账号密码
2. 检查 Token 生成逻辑
3. 确认认证接口 URL
```

**问题 3: 数据格式不匹配**
```
症状：JSON 解析失败
原因：HIS 返回格式与预期不符
解决：
1. 对比 HIS 接口文档
2. 调整 DTO 字段映射
3. 添加字段转换逻辑
```

**问题 4: 中文乱码**
```
症状：中文显示为乱码
原因：字符集配置不一致
解决：
1. 统一使用 UTF-8
2. 设置 HTTP 请求头 Content-Type: application/json; charset=utf-8
3. 检查数据库字符集
```

### 4.2 日志分析

**开启调试日志**:

```yaml
logging:
  level:
    com.hisagent.his: DEBUG
    org.springframework.web: DEBUG
```

**关键日志位置**:

- HIS 请求日志：`/var/log/his-agent/his-request.log`
- HIS 响应日志：`/var/log/his-agent/his-response.log`
- 异常日志：`/var/log/his-agent/error.log`

---

## 5. 联调报告模板

### 5.1 联调结果汇总

| 测试项 | 结果 | 备注 |
|--------|------|------|
| 基础连接 | ✅ 通过 / ❌ 失败 | - |
| 患者查询 | ✅ 通过 / ❌ 失败 | - |
| 问诊同步 | ✅ 通过 / ❌ 失败 | - |
| 数据一致性 | ✅ 通过 / ❌ 失败 | - |
| 异常处理 | ✅ 通过 / ❌ 失败 | - |
| 性能测试 | ✅ 通过 / ❌ 失败 | - |

### 5.2 遗留问题清单

| 问题描述 | 严重程度 | 责任人 | 预计解决时间 |
|----------|----------|--------|--------------|
| - | 高/中/低 | - | - |

### 5.3 联调结论

- [ ] 可以上线
- [ ] 需要整改后复测
- [ ] 存在重大问题，暂停集成

---

## 6. 联系支持

**技术支持**:
- 邮箱：support@his-agent.local
- 电话：400-XXX-XXXX

**文档更新**:
- 最后更新：2026-03-12
- 版本：1.0
