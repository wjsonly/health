import { createRouter, createWebHistory } from 'vue-router'

import AppointmentsPage from './pages/AppointmentsPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import LoginPage from './pages/LoginPage.vue'
import SchedulesPage from './pages/SchedulesPage.vue'
import ServiceItemsPage from './pages/ServiceItemsPage.vue'
import TherapistsPage from './pages/TherapistsPage.vue'
import { isAdminAuthenticated } from './auth/session'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginPage, meta: { public: true } },
    { path: '/', component: DashboardPage },
    { path: '/service-items', component: ServiceItemsPage },
    { path: '/therapists', component: TherapistsPage },
    { path: '/schedules', component: SchedulesPage },
    { path: '/appointments', component: AppointmentsPage },
  ],
})

router.beforeEach((to) => {
  const authenticated = isAdminAuthenticated()
  if (!to.meta.public && !authenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.path === '/login' && authenticated) {
    return (to.query.redirect as string) || '/'
  }
  return true
})
