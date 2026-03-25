# Docker Desktop 安装指南

## 📥 下载和安装

### 方法 1：自动打开浏览器（已尝试）
```bash
open "https://www.docker.com/products/docker-desktop/"
```

### 方法 2：手动访问
访问：https://www.docker.com/products/docker-desktop/

### 方法 3：使用 Homebrew Cask（需要 sudo 密码）
```bash
brew install --cask docker
```

---

## 🚀 安装步骤

### 1. 下载安装包
- 访问 Docker Desktop 官网
- 点击 "Download for Mac"
- 选择 Apple Silicon 版本 (M1/M2/M3 芯片)

### 2. 安装
1. 打开下载的 `.dmg` 文件
2. 拖动 Docker 图标到 Applications 文件夹
3. 打开 Applications 中的 Docker 应用

### 3. 首次启动
1. 打开 Docker Desktop 应用
2. 同意服务条款
3. 等待 Docker 引擎启动（状态栏图标变绿）

### 4. 验证安装
```bash
docker --version
docker-compose --version
docker ps
```

---

## ⚠️ 注意事项

### 需要管理员权限
Docker Desktop 安装需要输入 Mac 密码进行授权。

### 系统要求
- macOS 12.0+ (Monterey 或更高版本)
- Apple Silicon (M1/M2/M3) 或 Intel 芯片
- 4GB RAM 内存（推荐 8GB+）

---

## 🔧 安装后操作

### 启动 Docker 服务
```bash
# 确保 Docker Desktop 正在运行
# 状态栏 Docker 图标应该是绿色的

# 进入项目目录
cd /Users/yzh/opencode_workspace/his_agent

# 启动 MySQL 和 Redis
docker-compose -f docker-compose.dev.yml up -d

# 查看服务状态
docker-compose -f docker-compose.dev.yml ps

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f
```

---

## 📊 预期结果

成功启动后，你应该看到：
```
NAME                STATUS              PORTS
mysql               Up (healthy)        0.0.0.0:3306->3306/tcp
redis               Up (healthy)        0.0.0.0:6379->6379/tcp
```

---

## ❓ 常见问题

### Q: Docker 图标一直是灰色？
A: 等待几分钟让 Docker 引擎启动，或者重启 Docker Desktop 应用。

### Q: 提示权限不足？
A: 确保你使用的是有管理员权限的账户，输入正确的 Mac 密码。

### Q: 无法访问下载页面？
A: 可以尝试其他网络，或使用 Homebrew 安装。

---

## 📞 下一步

安装完成 Docker Desktop 后，继续执行项目的主安装流程：

1. ✅ Docker Desktop 安装完成
2. ⏭️ 启动 MySQL 和 Redis
3. ⏭️ 配置并启动后端
4. ⏭️ 安装前端依赖并启动

---

**最后更新**: 2026-03-22
