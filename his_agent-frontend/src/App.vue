<template>
  <div id="app" class="his-agent-app">
    <header class="app-header">
      <h1>his_agent - 医疗 AI 助手</h1>
    </header>
    
    <main class="app-main">
      <router-view />
    </main>
    
    <footer class="app-footer">
      <p>© 2026 his_agent. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
// App 根组件
</script>

<style scoped>
.his-agent-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #007bff;
  color: white;
  padding: 16px 24px;
}

.app-header h1 {
  margin: 0;
  font-size: 24px;
}

.app-main {
  flex: 1;
  padding: 24px;
}

.app-footer {
  background: #f8f9fa;
  padding: 16px 24px;
  text-align: center;
  color: #6c757d;
}
</style>
