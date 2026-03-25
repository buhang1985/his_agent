import { createRouter, createWebHistory } from 'vue-router';
import PatientDemo from './views/PatientDemo.vue';
import TestPage from './views/TestPage.vue';

const routes = [
  {
    path: '/',
    name: 'PatientDemo',
    component: PatientDemo,
  },
  {
    path: '/test',
    name: 'TestPage',
    component: TestPage,
  },
  {
    path: '/test/voice',
    name: 'VoiceConsultationTest',
    component: () => import('./views/VoiceConsultationTest.vue'),
  },
  {
    path: '/test/simple-voice',
    name: 'SimpleVoiceTest',
    component: () => import('./views/SimpleVoiceTest.vue'),
  },
  {
    path: '/test/xunfei-voice',
    name: 'XunfeiVoiceTest',
    component: () => import('./views/XunfeiVoiceTest.vue'),
  },
  {
    path: '/test/backend-voice',
    name: 'BackendProxyVoiceTest',
    component: () => import('./views/BackendProxyVoiceTest.vue'),
  },
  {
    path: '/test/voice-to-soap',
    name: 'VoiceToSoapTest',
    component: () => import('./views/VoiceToSoapTest.vue'),
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
