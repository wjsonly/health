<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'

import { authApi } from '../api/auth'
import { setAdminSession } from '../auth/session'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({
  username: 'admin',
  password: '',
})

async function submitLogin() {
  if (!form.username || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const response = await authApi.login({
      username: form.username,
      password: form.password,
    })
    setAdminSession(response.token, response.admin)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <span class="brand-mark">H</span>
        <div>
          <p class="eyebrow">Admin Console</p>
          <h1>健康管理后台</h1>
        </div>
      </div>

      <el-alert v-if="error" class="page-alert" type="error" :title="error" show-icon />

      <el-form class="login-form" @submit.prevent="submitLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号" autocomplete="username" size="large">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            placeholder="密码"
            type="password"
            autocomplete="current-password"
            size="large"
            show-password
            @keyup.enter="submitLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-button class="login-submit" type="primary" size="large" :loading="loading" @click="submitLogin">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>
