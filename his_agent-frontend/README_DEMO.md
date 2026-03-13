# HIS Agent 前后端 Demo

## 🚀 快速启动

### 1. 启动后端

```bash
cd /home/yuzihao/workspace/his_agent/his_agent-backend
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn spring-boot:run
```

后端将在 **http://localhost:8080** 启动

### 2. 启动前端

```bash
cd /home/yuzihao/workspace/his_agent/his_agent-frontend
npm install
npm run dev
```

前端将在 **http://localhost:5173** 启动

---

## 🎯 测试功能

### 页面功能
1. **健康检查** - 测试后端是否可访问
2. **加载患者列表** - 测试前后端数据通信

### API 端点
- `GET http://localhost:8080/api/patients/health` - 健康检查
- `GET http://localhost:8080/api/patients` - 获取患者列表

---

## ✅ 预期效果

### 成功标志
- [x] 前端页面正常显示
- [x] 点击"健康检查"显示成功消息
- [x] 点击"加载患者列表"显示 5 位患者数据
- [x] 数据表格正常渲染

### 患者数据
Demo 包含 5 位模拟患者：
- 张三（男，34 岁）
- 李四（女，39 岁）
- 王五（男，29 岁）
- 赵六（女，24 岁）
- 孙七（男，36 岁）

---

## 🔧 故障排查

### 后端启动失败
```bash
# 检查 Java 版本
java -version

# 检查端口占用
lsof -i :8080

# 查看日志
tail -f his_agent-backend/target/spring-boot.log
```

### 前端启动失败
```bash
# 检查 Node 版本
node -v

# 清除缓存重新安装
rm -rf node_modules package-lock.json
npm install
```

### 前后端通信失败
```bash
# 测试后端 API
curl http://localhost:8080/api/patients/health

# 检查 CORS 配置
# 确认后端 PatientController 有 @CrossOrigin 注解
```

---

## 📝 下一步

Demo 验证成功后，可以：
1. 继续开发其他业务功能
2. 连接真实数据库
3. 实现真实 HIS 集成
4. 部署到生产环境
