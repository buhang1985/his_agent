<template>
  <div class="dev-portal">
    <h1>DevPortal - 开发运维平台</h1>
    
    <div class="card">
      <h2>服务器状态</h2>
      <div v-if="serverStatus">
        <p>CPU: {{ serverStatus.cpuLoad }}</p>
        <p>内存：{{ formatBytes(serverStatus.memoryUsed) }} / {{ formatBytes(serverStatus.memoryMax) }}</p>
      </div>
    </div>
    
    <div class="card">
      <h2>服务健康度</h2>
      <div v-for="(status, service) in healthStatus" :key="service" class="health-item">
        <span :class="status.healthy ? 'healthy' : 'unhealthy'">{{ service }}</span>
        <span>{{ status.message }}</span>
      </div>
    </div>
    
    <div class="card">
      <h2>快捷操作</h2>
      <button @click="refresh" class="btn">🔄 刷新</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

interface ServerStatus {
  cpuLoad: number;
  memoryUsed: number;
  memoryMax: number;
}

interface HealthStatus {
  healthy: boolean;
  message: string;
}

const serverStatus = ref<ServerStatus | null>(null);
const healthStatus = ref<Record<string, HealthStatus>>({});

const fetchServerStatus = async () => {
  const response = await fetch('/api/dev/server-status');
  serverStatus.value = await response.json();
};

const fetchHealthStatus = async () => {
  const response = await fetch('/api/dev/health');
  healthStatus.value = await response.json();
};

const refresh = () => {
  fetchServerStatus();
  fetchHealthStatus();
};

const formatBytes = (bytes: number) => {
  const mb = bytes / 1024 / 1024;
  return `${mb.toFixed(2)} MB`;
};

onMounted(() => {
  refresh();
  setInterval(refresh, 30000);
});
</script>

<style scoped>
.dev-portal {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.health-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e5e7eb;
}

.healthy {
  color: #22c55e;
  font-weight: bold;
}

.unhealthy {
  color: #ef4444;
  font-weight: bold;
}

.btn {
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn:hover {
  background: #0056b3;
}
</style>
