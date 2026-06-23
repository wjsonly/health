<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { adminApi, type AdminAppointment, type AdminTherapist, type ServiceItem } from '../api/admin'

const loading = ref(false)
const error = ref('')
const appointments = ref<AdminAppointment[]>([])
const therapists = ref<AdminTherapist[]>([])
const serviceItems = ref<ServiceItem[]>([])

const today = new Date().toLocaleDateString('en-CA')

const activeServiceItems = computed(() =>
  serviceItems.value.filter((item) => ['ACTIVE', 'ON_SHELF', 'ENABLED'].includes(item.status)),
)

const activeTherapists = computed(() =>
  therapists.value.filter((therapist) => therapist.status !== 'INACTIVE' && therapist.visible),
)

const serviceNameMap = computed(() => new Map(serviceItems.value.map((item) => [item.id, item.name])))
const therapistNameMap = computed(() => new Map(therapists.value.map((item) => [item.id, item.name])))

const stats = computed(() => [
  { label: '今日预约数', value: appointments.value.length },
  { label: '待到店', value: countAppointments('BOOKED') },
  { label: '服务中', value: countAppointments('IN_SERVICE') },
  { label: '已完成', value: countAppointments('COMPLETED') },
  { label: '已取消', value: countAppointments('CANCELLED') },
  { label: '技师数量', value: activeTherapists.value.length },
  { label: '上架项目数量', value: activeServiceItems.value.length },
])

const actionAppointments = computed(() =>
  appointments.value
    .filter((appointment) => ['BOOKED', 'ARRIVED', 'IN_SERVICE'].includes(appointment.status))
    .slice(0, 6)
    .map((appointment) => ({
      ...appointment,
      serviceName: serviceNameMap.value.get(appointment.serviceItemId) || `项目 #${appointment.serviceItemId}`,
      therapistName: therapistNameMap.value.get(appointment.therapistId) || `技师 #${appointment.therapistId}`,
      statusText: statusLabel(appointment.status),
      timeText: `${appointment.startTime.slice(0, 5)}-${appointment.endTime.slice(0, 5)}`,
    })),
)

function countAppointments(status: string) {
  return appointments.value.filter((appointment) => appointment.status === status).length
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    BOOKED: '待到店',
    ARRIVED: '已到店',
    IN_SERVICE: '服务中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return labels[status] || status
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [appointmentList, therapistList, serviceItemList] = await Promise.all([
      adminApi.getAppointments(today),
      adminApi.getTherapists(),
      adminApi.getServiceItems(),
    ])
    appointments.value = appointmentList
    therapists.value = therapistList
    serviceItems.value = serviceItemList
  } catch (err) {
    error.value = err instanceof Error ? err.message : '数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <section class="page-panel">
    <div class="page-heading">
      <div>
        <p class="page-kicker">数据看板</p>
        <h2>运营概览</h2>
      </div>
      <el-button :loading="loading" @click="loadDashboard">刷新</el-button>
    </div>

    <el-alert v-if="error" class="page-alert" type="error" :title="error" show-icon />

    <el-skeleton :loading="loading" animated :rows="4">
      <div class="stat-grid">
        <div v-for="stat in stats" :key="stat.label" class="stat-cell">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
      </div>

      <div class="dashboard-section">
        <div class="section-heading">
          <div>
            <p class="page-kicker">今日待处理</p>
            <h3>需要跟进的预约</h3>
          </div>
        </div>
        <el-empty v-if="!actionAppointments.length" description="今日暂无待处理预约" />
        <div v-else class="action-list">
          <div v-for="appointment in actionAppointments" :key="appointment.id" class="action-row">
            <div>
              <strong>{{ appointment.contactName }}</strong>
              <span>{{ appointment.serviceName }} / {{ appointment.therapistName }}</span>
            </div>
            <div class="action-meta">
              <span>{{ appointment.timeText }}</span>
              <el-tag size="small">{{ appointment.statusText }}</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-skeleton>
  </section>
</template>
