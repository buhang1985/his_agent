<template>
  <div class="test-page">
    <el-card class="header-card">
      <h1>🧪 系统测试页面</h1>
      <p class="subtitle">测试数据库 CRUD、Redis 缓存和前后端通信</p>
    </el-card>

    <!-- 综合测试 -->
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <span>🎯 综合测试</span>
          <el-tag type="success">推荐</el-tag>
        </div>
      </template>
      <p>一键测试数据库、Redis 和前后端通信</p>
      <el-button type="primary" @click="runComprehensiveTest" :loading="comprehensiveLoading">
        运行综合测试
      </el-button>
      
      <div v-if="comprehensiveResult" class="result-box">
        <pre>{{ JSON.stringify(comprehensiveResult, null, 2) }}</pre>
      </div>
    </el-card>

    <!-- 数据库测试 -->
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <span>🗄️ 数据库 CRUD 测试</span>
          <el-tag type="warning">MySQL</el-tag>
        </div>
      </template>
      
      <el-space direction="vertical" :size="10" style="width: 100%">
        <el-button @click="testDbConnection" :loading="dbLoading">
          1️⃣ 测试数据库连接
        </el-button>
        
        <el-button @click="createTestPatients" :loading="dbLoading">
          2️⃣ 批量插入测试数据
        </el-button>
        
        <el-button @click="getAllPatients" :loading="dbLoading">
          3️⃣ 查询所有患者
        </el-button>
        
        <el-button @click="createSinglePatient" :loading="dbLoading">
          4️⃣ 创建单个患者
        </el-button>
        
        <el-button @click="updatePatient" :loading="dbLoading">
          5️⃣ 更新患者
        </el-button>
        
        <el-button @click="deletePatient" :loading="dbLoading" type="danger">
          6️⃣ 删除患者
        </el-button>
      </el-space>
      
      <div v-if="dbResult" class="result-box">
        <pre>{{ JSON.stringify(dbResult, null, 2) }}</pre>
      </div>
    </el-card>

    <!-- Redis 缓存测试 -->
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <span>💾 Redis 缓存测试</span>
          <el-tag type="danger">Redis</el-tag>
        </div>
      </template>
      
      <el-space direction="vertical" :size="10" style="width: 100%">
        <el-button @click="testRedisConnection" :loading="redisLoading">
          1️⃣ 测试 Redis 连接
        </el-button>
        
        <el-button @click="setCache" :loading="redisLoading">
          2️⃣ 设置缓存
        </el-button>
        
        <el-button @click="getCache" :loading="redisLoading">
          3️⃣ 获取缓存
        </el-button>
        
        <el-button @click="deleteCache" :loading="redisLoading" type="danger">
          4️⃣ 删除缓存
        </el-button>
        
        <el-button @click="testCachePenetration" :loading="redisLoading">
          5️⃣ 测试缓存穿透防护
        </el-button>
        
        <el-button @click="testCacheAvalanche" :loading="redisLoading">
          6️⃣ 测试缓存雪崩防护
        </el-button>
        
        <el-button @click="getCacheList" :loading="redisLoading">
          7️⃣ 查看所有测试缓存
        </el-button>
        
        <el-button @click="clearTestCache" :loading="redisLoading" type="danger">
          8️⃣ 清空所有测试缓存
        </el-button>
      </el-space>
      
      <div v-if="redisResult" class="result-box">
        <pre>{{ JSON.stringify(redisResult, null, 2) }}</pre>
      </div>
    </el-card>

    <!-- 前后端通信测试 -->
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <span>🔗 前后端通信测试</span>
          <el-tag type="info">HTTP</el-tag>
        </div>
      </template>
      
      <el-space direction="vertical" :size="10" style="width: 100%">
        <el-button @click="testPing" :loading="commLoading">
          1️⃣ Ping 测试
        </el-button>
        
        <el-button @click="testLatency" :loading="commLoading">
          2️⃣ 延迟测试
        </el-button>
      </el-space>
      
      <div v-if="commResult" class="result-box">
        <pre>{{ JSON.stringify(commResult, null, 2) }}</pre>
      </div>
    </el-card>

    <!-- 患者列表 -->
    <el-card class="test-card" v-if="patients.length > 0">
      <template #header>
        <span>📋 患者列表</span>
      </template>
      
      <el-table :data="patients" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="200" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="address" label="地址" />
        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button size="small" @click="selectPatient(scope.row)">
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { httpService } from '@/services/http';

interface Patient {
  id: string;
  name: string;
  phone: string;
  gender: string;
  age: number;
  address: string;
}

// 加载状态
const comprehensiveLoading = ref(false);
const dbLoading = ref(false);
const redisLoading = ref(false);
const commLoading = ref(false);

// 结果数据
const comprehensiveResult = ref<any>(null);
const dbResult = ref<any>(null);
const redisResult = ref<any>(null);
const commResult = ref<any>(null);
const patients = ref<Patient[]>([]);
const selectedPatient = ref<Patient | null>(null);

// ==================== 综合测试 ====================
const runComprehensiveTest = async () => {
  comprehensiveLoading.value = true;
  try {
    const response = await httpService.get('/test/comprehensive');
    comprehensiveResult.value = response.data.data;
    ElMessage.success('综合测试成功！');
  } catch (error: any) {
    ElMessage.error('综合测试失败：' + error.message);
  } finally {
    comprehensiveLoading.value = false;
  }
};

// ==================== 数据库测试 ====================
const testDbConnection = async () => {
  dbLoading.value = true;
  try {
    const response = await httpService.get('/test/db/connection');
    dbResult.value = response.data.data;
    ElMessage.success('数据库连接正常！');
  } catch (error: any) {
    ElMessage.error('数据库连接失败：' + error.message);
  } finally {
    dbLoading.value = false;
  }
};

const createTestPatients = async () => {
  dbLoading.value = true;
  try {
    const response = await httpService.post('/test/db/batch-insert');
    dbResult.value = response.data.data;
    ElMessage.success(`成功插入 ${response.data.data.inserted} 条测试数据`);
    getAllPatients();
  } catch (error: any) {
    ElMessage.error('批量插入失败：' + error.message);
  } finally {
    dbLoading.value = false;
  }
};

const getAllPatients = async () => {
  dbLoading.value = true;
  try {
    const response = await httpService.get('/test/db/patients');
    patients.value = response.data.data;
    dbResult.value = { count: patients.value.length, data: patients.value };
    ElMessage.success(`查询到 ${patients.value.length} 位患者`);
  } catch (error: any) {
    ElMessage.error('查询失败：' + error.message);
  } finally {
    dbLoading.value = false;
  }
};

const createSinglePatient = async () => {
  dbLoading.value = true;
  try {
    const patient = {
      id: `test_${Date.now()}`,
      name: `测试用户_${Date.now()}`,
      idCard: '110101199001011234',
      phone: `138${Date.now().toString().slice(-8)}`,
      gender: '男',
      age: 30,
      address: '北京市测试区'
    };
    const response = await httpService.post('/test/db/patient', patient);
    dbResult.value = response.data.data;
    ElMessage.success('创建成功！');
    getAllPatients();
  } catch (error: any) {
    ElMessage.error('创建失败：' + error.message);
  } finally {
    dbLoading.value = false;
  }
};

const updatePatient = async () => {
  if (patients.value.length === 0) {
    ElMessage.warning('请先查询患者');
    return;
  }
  
  dbLoading.value = true;
  try {
    const patient = patients.value[0];
    patient.name = `更新_${Date.now()}`;
    const response = await httpService.put(`/test/db/patient/${patient.id}`, patient);
    dbResult.value = response.data.data;
    ElMessage.success('更新成功！');
    getAllPatients();
  } catch (error: any) {
    ElMessage.error('更新失败：' + error.message);
  } finally {
    dbLoading.value = false;
  }
};

const deletePatient = async () => {
  if (patients.value.length === 0) {
    ElMessage.warning('请先查询患者');
    return;
  }
  
  dbLoading.value = true;
  try {
    const patient = patients.value[0];
    await httpService.delete(`/test/db/patient/${patient.id}`);
    dbResult.value = { message: '删除成功', id: patient.id };
    ElMessage.success('删除成功！');
    getAllPatients();
  } catch (error: any) {
    ElMessage.error('删除失败：' + error.message);
  } finally {
    dbLoading.value = false;
  }
};

// ==================== Redis 测试 ====================
const testRedisConnection = async () => {
  redisLoading.value = true;
  try {
    const response = await httpService.get('/test/redis/connection');
    redisResult.value = response.data.data;
    ElMessage.success('Redis 连接正常！');
  } catch (error: any) {
    ElMessage.error('Redis 连接失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const setCache = async () => {
  redisLoading.value = true;
  try {
    const key = `test:manual:${Date.now()}`;
    const value = `value_${Date.now()}`;
    const response = await httpService.post('/test/redis/cache', null, {
      params: { key, value, ttl: 300 }
    });
    redisResult.value = { key, value, message: response.data.data };
    ElMessage.success('缓存设置成功！');
  } catch (error: any) {
    ElMessage.error('设置缓存失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const getCache = async () => {
  redisLoading.value = true;
  try {
    const keys = await httpService.get('/test/redis/cache-list');
    if (keys.data.data.count === 0) {
      ElMessage.warning('没有测试缓存');
      return;
    }
    const key = keys.data.data.keys[0];
    const response = await httpService.get(`/test/redis/cache/${encodeURIComponent(key)}`);
    redisResult.value = { key, value: response.data.data };
    ElMessage.success('获取缓存成功！');
  } catch (error: any) {
    ElMessage.error('获取缓存失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const deleteCache = async () => {
  redisLoading.value = true;
  try {
    const keys = await httpService.get('/test/redis/cache-list');
    if (keys.data.data.count === 0) {
      ElMessage.warning('没有测试缓存');
      return;
    }
    const key = keys.data.data.keys[0];
    await httpService.delete(`/test/redis/cache/${encodeURIComponent(key)}`);
    redisResult.value = { key, message: '删除成功' };
    ElMessage.success('删除缓存成功！');
  } catch (error: any) {
    ElMessage.error('删除缓存失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const testCachePenetration = async () => {
  redisLoading.value = true;
  try {
    const response = await httpService.get('/test/redis/cache-penetration');
    redisResult.value = response.data.data;
    ElMessage.success(response.data.data.message);
  } catch (error: any) {
    ElMessage.error('测试失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const testCacheAvalanche = async () => {
  redisLoading.value = true;
  try {
    const response = await httpService.get('/test/redis/cache-avalanche');
    redisResult.value = response.data.data;
    ElMessage.success(response.data.data.message);
  } catch (error: any) {
    ElMessage.error('测试失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const getCacheList = async () => {
  redisLoading.value = true;
  try {
    const response = await httpService.get('/test/redis/cache-list');
    redisResult.value = response.data.data;
    ElMessage.success(`共有 ${response.data.data.count} 个测试缓存`);
  } catch (error: any) {
    ElMessage.error('获取缓存列表失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

const clearTestCache = async () => {
  redisLoading.value = true;
  try {
    await httpService.delete('/test/redis/cache-clear');
    redisResult.value = { message: '清空成功' };
    ElMessage.success('清空所有测试缓存成功！');
  } catch (error: any) {
    ElMessage.error('清空缓存失败：' + error.message);
  } finally {
    redisLoading.value = false;
  }
};

// ==================== 通信测试 ====================
const testPing = async () => {
  commLoading.value = true;
  try {
    const response = await httpService.get('/test/communication/ping');
    commResult.value = response.data.data;
    ElMessage.success('后端响应正常！');
  } catch (error: any) {
    ElMessage.error('通信失败：' + error.message);
  } finally {
    commLoading.value = false;
  }
};

const testLatency = async () => {
  commLoading.value = true;
  try {
    const response = await httpService.get('/test/communication/latency');
    commResult.value = response.data.data;
    ElMessage.success(`响应时间：${response.data.data.latency_ms}ms`);
  } catch (error: any) {
    ElMessage.error('延迟测试失败：' + error.message);
  } finally {
    commLoading.value = false;
  }
};

const selectPatient = (patient: Patient) => {
  selectedPatient.value = patient;
  ElMessage.success(`已选择患者：${patient.name}`);
};
</script>

<style scoped>
.test-page {
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
}

.test-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-box {
  margin-top: 15px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 400px;
  overflow-y: auto;
}

.result-box pre {
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-wrap: break-word;
}

:deep(.el-button) {
  width: 100%;
  margin-bottom: 5px;
}
</style>
