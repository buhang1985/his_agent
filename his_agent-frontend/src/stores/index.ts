import { createPinia } from 'pinia';
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';

/**
 * Pinia 配置
 */
const pinia = createPinia();

// 注册持久化插件
pinia.use(piniaPluginPersistedstate);

export default pinia;
