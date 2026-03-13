<template>
  <div class="patient-demo">
    <el-card class="header-card">
      <h1>🏥 HIS Agent 患者管理演示</h1>
      <p class="subtitle">前后端通信测试页面</p>
      
      <el-button 
        type="primary" 
        @click="loadPatients" 
        :loading="loading"
        size="large"
      >
        🔄 加载患者列表
      </el-button>
      
      <el-button 
        @click="testHealth" 
        :loading="healthChecking"
        size="large"
      >
        💚 健康检查
      </el-button>
    </el-card>

    <!-- 状态提示 -->
    <el-alert
      v-if="backendStatus"
      :title="backendStatus"
      :type="backendStatus.includes('成功') ? 'success' : 'error'"
      :closable="false"
      show-icon
      class="status-alert"
    />

    <!-- 患者列表 -->
    <el-card class="patient-card" v-if="patients.length > 0">
      <template #header>
        <div class="card-header">
          <span>📋 患者列表</span>
          <el-tag type="info">{{ patients.length }} 位患者</el-tag>
        </div>
      </template>

      <el-table :data="patients" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="idCard" label="身份证号" />
        <el-table-column prop="address" label="地址" />
      </el-table>
    </el-card>

    <!-- 空状态 -->
    <el-empty 
      v-else 
      description="点击加载患者列表按钮查看数据"
      :image-size="200"
    >
      <el-image 
        src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ctext y='.9em' font-size='90'%3E🏥%3C/text%3E%3C/svg%3E" 
        style="width: 100px; height: 100px"
      />
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { httpService } from '@/services/http';

interface Patient {
  id: string;
  name: string;
  gender: string;
  age: number;
  phone: string;
  idCard: string;
  address: string;
}

const patients = ref<Patient[]>([]);
const loading = ref(false);
const healthChecking = ref(false);
const backendStatus = ref('');

// 加载患者列表
const loadPatients = async () => {
  loading.value = true;
  backendStatus.value = '';
  
  try {
    const response = await httpService.get('/patients');
    patients.value = response.data.data;
    backendStatus.value = '✅ 后端连接成功！数据加载完成';
    ElMessage.success(`成功加载 ${patients.value.length} 位患者信息`);
  } catch (error: any) {
    backendStatus.value = '❌ 后端连接失败：' + (error.message || '未知错误');
    ElMessage.error('加载失败，请检查后端服务是否启动');
    console.error('Load patients failed:', error);
  } finally {
    loading.value = false;
  }
};

// 健康检查
const testHealth = async () => {
  healthChecking.value = true;
  
  try {
    const response = await httpService.get('/patients/health');
    backendStatus.value = '✅ ' + response.data.data;
    ElMessage.success('后端服务运行正常！');
  } catch (error: any) {
    backendStatus.value = '❌ 健康检查失败：' + (error.message || '未知错误');
    ElMessage.error('后端服务可能未启动');
    console.error('Health check failed:', error);
  } finally {
    healthChecking.value = false;
  }
};
</script>

<style scoped>
.patient-demo {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
  text-align: center;
}

.header-card h1 {
  margin: 0 0 10px 0;
  color: #409EFF;
}

.subtitle {
  color: #909399;
  margin-bottom: 20px;
}

.status-alert {
  margin-bottom: 20px;
}

.patient-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}
</style>
