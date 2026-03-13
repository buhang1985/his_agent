# 🏥 HIS Agent Demo - 前后端通信测试

## 🚀 快速启动（推荐）

### 方式一：一键启动脚本

```bash
cd /home/yuzihao/workspace/his_agent
./start-demo.sh
```

### 方式二：手动启动

#### 1. 启动后端

```bash
cd /home/yuzihao/workspace/his_agent/his_agent-backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn spring-boot:run
```

#### 2. 启动前端（新终端）

```bash
cd /home/yuzihao/workspace/his_agent/his_agent-frontend
npm run dev
```

---

## 🎯 访问页面

启动成功后，在浏览器访问：

**http://localhost:5173**

---

## ✅ 测试功能

### 1. 健康检查
点击 **"💚 健康检查"** 按钮

**预期结果**：
- 显示绿色成功提示
- 消息："HIS Agent Backend is running!"

### 2. 加载患者列表
点击 **"🔄 加载患者列表"** 按钮

**预期结果**：
- 显示 5 位患者数据的表格
- 包含：ID、姓名、性别、年龄、手机号、身份证号、地址

---

## 📊 患者数据示例

| ID | 姓名 | 性别 | 年龄 | 手机号 | 身份证号 | 地址 |
|----|------|------|------|--------|----------|------|
| P001 | 张三 | 男 | 34 | 138****5678 | 110101199001011234 | 北京市朝阳区 |
| P002 | 李四 | 女 | 39 | 139****9999 | 110101198502022345 | 北京市海淀区 |
| P003 | 王五 | 男 | 29 | 137****8888 | 110101199503033456 | 北京市西城区 |
| P004 | 赵六 | 女 | 24 | 136****7777 | 110101200004044567 | 北京市东城区 |
| P005 | 孙七 | 男 | 36 | 135****6666 | 110101198805055678 | 北京市丰台区 |

---

## 🔧 故障排查

### 后端启动失败

**检查 Java 版本**：
```bash
java -version
# 应该显示 Java 17
```

**检查端口占用**：
```bash
lsof -i :8080
# 如果端口被占用，杀掉进程或修改配置
```

**查看日志**：
```bash
tail -f /tmp/his-backend.log
```

### 前端启动失败

**检查 Node 版本**：
```bash
node -v
# 应该显示 v18+
```

**重新安装依赖**：
```bash
cd /home/yuzihao/workspace/his_agent/his_agent-frontend
rm -rf node_modules package-lock.json
npm install
```

**查看日志**：
```bash
tail -f /tmp/his-frontend.log
```

### 前后端通信失败

**测试后端 API**：
```bash
curl http://localhost:8080/api/patients/health
# 应该返回：{"code":200,"message":"success","data":"HIS Agent Backend is running!"}
```

**检查 CORS**：
- 确认后端 `PatientController.java` 有 `@CrossOrigin(origins = "*")` 注解

---

## 🛑 停止服务

```bash
# 停止后端
kill $(cat /tmp/his-backend.pid)

# 停止前端
kill $(cat /tmp/his-frontend.pid)
```

---

## 📁 项目结构

```
his_agent/
├── his_agent-backend/          # 后端项目
│   ├── src/main/java/.../
│   │   ├── controller/
│   │   │   └── PatientController.java  # 患者控制器
│   │   ├── dto/
│   │   │   └── PatientDTO.java         # 患者 DTO
│   │   └── service/
│   │       └── PatientService.java     # 患者服务
│   └── pom.xml
│
├── his_agent-frontend/         # 前端项目
│   ├── src/
│   │   ├── views/
│   │   │   └── PatientDemo.vue         # 患者演示页面
│   │   ├── services/
│   │   │   └── http.ts                 # HTTP 客户端
│   │   └── App.vue                     # 主应用
│   └── package.json
│
├── start-demo.sh               # 一键启动脚本
└── README_DEMO.md              # 本文档
```

---

## 🎉 成功标志

当你看到以下界面时，说明前后端通信正常：

```
┌─────────────────────────────────────────┐
│  🏥 HIS Agent 患者管理演示               │
│  前后端通信测试页面                      │
│                                         │
│  [🔄 加载患者列表]  [💚 健康检查]        │
│                                         │
│  ✅ 后端连接成功！数据加载完成           │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │ 📋 患者列表           5 位患者    │   │
│  ├─────────────────────────────────┤   │
│  │ ID  │姓名│性别│年龄│手机│身份证...│   │
│  ├─────────────────────────────────┤   │
│  │ P001│张三│男  │34  │... │...     │   │
│  │ P002│李四│女  │39  │... │...     │   │
│  │ ...                              │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## 📝 下一步

Demo 验证成功后，可以：
1. 继续开发其他业务功能
2. 连接真实数据库
3. 实现真实 HIS 集成
4. 部署到生产环境

**祝使用愉快！** 🎊
