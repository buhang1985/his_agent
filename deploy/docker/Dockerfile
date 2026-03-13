FROM eclipse-temurin:17-jdk-alpine

# 安装运行时依赖
RUN apk add --no-cache \
    mysql-client \
    redis-tools \
    curl \
    tzdata

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户
RUN addgroup -g 1000 hisagent && \
    adduser -D -u 1000 -G hisagent hisagent

WORKDIR /app

# 复制应用 JAR
COPY target/his-agent-backend-0.1.0-SNAPSHOT.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && \
    chown -R hisagent:hisagent /app

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 切换到非 root 用户
USER hisagent

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-Xmx1g", \
  "-Xms1g", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/app/logs", \
  "-jar", "app.jar"]
