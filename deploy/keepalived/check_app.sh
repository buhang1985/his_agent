#!/bin/bash
# /usr/local/bin/check_app.sh
# his_agent 应用健康检查脚本

response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)

if [ "$response" -eq 200 ]; then
    exit 0
else
    exit 1
fi
