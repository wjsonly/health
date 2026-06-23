<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, DataBoard, PriceTag, SwitchButton, Tickets, User } from '@element-plus/icons-vue'

import { clearAdminSession, getStoredAdmin, type StoredAdmin } from './auth/session'

const route = useRoute()
const router = useRouter()
const admin = ref<StoredAdmin | null>(getStoredAdmin())

const navItems = [
  { path: '/', label: '数据看板', icon: DataBoard },
  { path: '/service-items', label: '项目管理', icon: PriceTag },
  { path: '/therapists', label: '技师管理', icon: User },
  { path: '/schedules', label: '排班管理', icon: Calendar },
  { path: '/appointments', label: '预约订单', icon: Tickets },
]

watch(
  () => route.fullPath,
  () => {
    admin.value = getStoredAdmin()
  },
  { immediate: true },
)

function logout() {
  clearAdminSession()
  router.replace('/login')
}
</script>

<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="admin-shell">
    <el-aside class="admin-sidebar" width="208px">
      <div class="brand">
        <span class="brand-mark">H</span>
        <span class="brand-name">健康管理后台</span>
      </div>
      <el-menu class="admin-menu" router :default-active="$route.path">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <p class="eyebrow">Admin Console</p>
          <h1>运营管理</h1>
        </div>
        <div class="admin-user">
          <span>{{ admin?.displayName || '管理员' }}</span>
          <el-button :icon="SwitchButton" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
