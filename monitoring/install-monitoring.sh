#!/bin/bash
# =====================================================
# HIS Agent 监控系统安装脚本
# 安装：Prometheus + Grafana + AlertManager + Node Exporter
# =====================================================

set -e

MONITORING_DIR="/home/yuzihao/workspace/his_agent/monitoring"
INSTALL_DIR="/mnt/d/his-agent-monitoring"

echo "=========================================="
echo "HIS Agent 监控系统安装"
echo "=========================================="

# 创建安装目录
echo "[1/8] 创建安装目录..."
sudo mkdir -p $INSTALL_DIR/{prometheus,grafana,alertmanager,node_exporter,data}

# 下载 Prometheus
echo "[2/8] 下载 Prometheus..."
cd $INSTALL_DIR
if [ ! -f "prometheus-2.50.1.linux-amd64.tar.gz" ]; then
    wget -q https://github.com/prometheus/prometheus/releases/download/v2.50.1/prometheus-2.50.1.linux-amd64.tar.gz
    tar xzf prometheus-2.50.1.linux-amd64.tar.gz
    mv prometheus-2.50.1.linux-amd64/* prometheus/
    rm -rf prometheus-2.50.1.linux-amd64 prometheus-2.50.1.linux-amd64.tar.gz
fi

# 下载 Grafana
echo "[3/8] 下载 Grafana..."
if [ ! -f "grafana-10.3.3.linux-amd64.tar.gz" ]; then
    wget -q https://dl.grafana.com/oss/release/grafana-10.3.3.linux-amd64.tar.gz
    tar xzf grafana-10.3.3.linux-amd64.tar.gz
    mv grafana-v10.3.3/* grafana/
    rm -rf grafana-v10.3.3 grafana-10.3.3.linux-amd64.tar.gz
fi

# 下载 AlertManager
echo "[4/8] 下载 AlertManager..."
if [ ! -f "alertmanager-0.26.0.linux-amd64.tar.gz" ]; then
    wget -q https://github.com/prometheus/alertmanager/releases/download/v0.26.0/alertmanager-0.26.0.linux-amd64.tar.gz
    tar xzf alertmanager-0.26.0.linux-amd64.tar.gz
    mv alertmanager-0.26.0.linux-amd64/* alertmanager/
    rm -rf alertmanager-0.26.0.linux-amd64 alertmanager-0.26.0.linux-amd64.tar.gz
fi

# 下载 Node Exporter
echo "[5/8] 下载 Node Exporter..."
if [ ! -f "node_exporter-1.7.0.linux-amd64.tar.gz" ]; then
    wget -q https://github.com/prometheus/node_exporter/releases/download/v1.7.0/node_exporter-1.7.0.linux-amd64.tar.gz
    tar xzf node_exporter-1.7.0.linux-amd64.tar.gz
    mv node_exporter-1.7.0.linux-amd64/* node_exporter/
    rm -rf node_exporter-1.7.0.linux-amd64 node_exporter-1.7.0.linux-amd64.tar.gz
fi

# 复制配置文件
echo "[6/8] 复制配置文件..."
cp $MONITORING_DIR/prometheus/prometheus.yml $INSTALL_DIR/prometheus/
cp -r $MONITORING_DIR/prometheus/alerts $INSTALL_DIR/prometheus/
cp $MONITORING_DIR/alertmanager/alertmanager.yml $INSTALL_DIR/alertmanager/

# 创建 systemd 服务
echo "[7/8] 创建 systemd 服务..."

# Prometheus 服务
sudo cat > /etc/systemd/system/prometheus.service << 'EOF'
[Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/opt/his-agent-monitoring/prometheus/prometheus \
  --config.file=/opt/his-agent-monitoring/prometheus/prometheus.yml \
  --storage.tsdb.path=/opt/his-agent-monitoring/data/prometheus \
  --storage.tsdb.retention.time=15d \
  --web.enable-lifecycle \
  --web.enable-admin-api
ExecReload=/bin/kill -HUP $MAINPID
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# Grafana 服务
sudo cat > /etc/systemd/system/grafana.service << 'EOF'
[Unit]
Description=Grafana
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
Environment="GF_PATHS_DATA=/opt/his-agent-monitoring/data/grafana"
Environment="GF_PATHS_LOGS=/opt/his-agent-monitoring/logs/grafana"
Environment="GF_PATHS_PLUGINS=/opt/his-agent-monitoring/grafana/plugins"
ExecStart=/opt/his-agent-monitoring/grafana/bin/grafana-server \
  --homepath=/opt/his-agent-monitoring/grafana \
  --config=/opt/his-agent-monitoring/grafana/conf/defaults.ini
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# AlertManager 服务
sudo cat > /etc/systemd/system/alertmanager.service << 'EOF'
[Unit]
Description=AlertManager
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/opt/his-agent-monitoring/alertmanager/alertmanager \
  --config.file=/opt/his-agent-monitoring/alertmanager/alertmanager.yml \
  --storage.path=/opt/his-agent-monitoring/data/alertmanager \
  --web.external-url=http://localhost:9093
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# Node Exporter 服务
sudo cat > /etc/systemd/system/node_exporter.service << 'EOF'
[Unit]
Description=Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/opt/his-agent-monitoring/node_exporter/node_exporter
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
echo "[8/8] 启动服务..."
sudo systemctl enable prometheus
sudo systemctl enable grafana
sudo systemctl enable alertmanager
sudo systemctl enable node_exporter

sudo systemctl start prometheus
sudo systemctl start grafana
sudo systemctl start alertmanager
sudo systemctl start node_exporter

# 验证服务
echo ""
echo "=========================================="
echo "安装完成！服务状态："
echo "=========================================="
systemctl is-active prometheus
systemctl is-active grafana
systemctl is-active alertmanager
systemctl is-active node_exporter

echo ""
echo "=========================================="
echo "访问地址："
echo "=========================================="
echo "Prometheus:   http://localhost:9090"
echo "Grafana:      http://localhost:3000 (admin/admin)"
echo "AlertManager: http://localhost:9093"
echo "Node Exporter: http://localhost:9100/metrics"
echo ""
echo "日志查看：journalctl -u prometheus -f"
echo "=========================================="
