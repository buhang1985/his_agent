import { createRouter, createWebHistory } from 'vue-router';
import PatientDemo from './views/PatientDemo.vue';

const routes = [
  {
    path: '/',
    name: 'PatientDemo',
    component: PatientDemo,
  },
  {
    path: '/__dev',
    name: 'DevPortal',
    component: () => import('./views/DevPortal.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
