<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { adminApi, type AdminAppointment, type AdminTherapist, type ServiceItem } from '../api/admin'

const loading = ref(false)
const actionLoadingId = ref<number | null>(null)
const error = ref('')
const selectedDate = ref(new Date().toLocaleDateString('en-CA'))
const statusFilter = ref('ALL')
const appointments = ref<AdminAppointment[]>([])
const serviceItems = ref<ServiceItem[]>([])
const therapists = ref<AdminTherapist[]>([])

const statusLabels: Record<string, string> = {
  BOOKED: '已预约',
  ARRIVED: '已到店',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '已预约', value: 'BOOKED' },
  { label: '已到店', value: 'ARRIVED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

const serviceNameMap = computed(() => new Map(serviceItems.value.map((item) => [item.id, item.name])))
const therapistNameMap = computed(() => new Map(therapists.value.map((item) => [item.id, item.name])))

const appointmentRows = computed(() =>
  appointments.value.map((appointment) => ({
    ...appointment,
    serviceName: serviceNameMap.value.get(appointment.serviceItemId) || `项目 #${appointment.serviceItemId}`,
    therapistName: therapistNameMap.value.get(appointment.therapistId) || `技师 #${appointment.therapistId}`,
  })),
)

const filteredRows = computed(() => {
  if (statusFilter.value === 'ALL') {
    return appointmentRows.value
  }
  return appointmentRows.value.filter((appointment) => appointment.status === statusFilter.value)
})

function statusType(status: string) {
  const types: Record<string, string> = {
    BOOKED: 'primary',
    ARRIVED: 'warning',
    IN_SERVICE: 'success',
    COMPLETED: 'info',
    CANCELLED: 'danger',
  }
  return types[status] || 'info'
}

function money(value: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

async function loadAppointments() {
  loading.value = true
  error.value = ''
  try {
    const [appointmentList, therapistList, serviceItemList] = await Promise.all([
      adminApi.getAppointments(selectedDate.value),
      adminApi.getTherapists(),
      adminApi.getServiceItems(),
    ])
    appointments.value = appointmentList
    therapists.value = therapistList
    serviceItems.value = serviceItemList
  } catch (err) {
    error.value = err instanceof Error ? err.message : '预约列表加载失败'
  } finally {
    loading.value = false
  }
}

function actionOptions(appointment: AdminAppointment) {
  if (appointment.status === 'BOOKED') {
    return [
      { label: '到店', type: 'primary' as const, action: 'arrive' as const },
      { label: '取消', type: 'danger' as const, action: 'cancel' as const },
    ]
  }
  if (appointment.status === 'ARRIVED') {
    return [
      { label: '开始', type: 'success' as const, action: 'start' as const },
      { label: '取消', type: 'danger' as const, action: 'cancel' as const },
    ]
  }
  if (appointment.status === 'IN_SERVICE') {
    return [{ label: '完成', type: 'primary' as const, action: 'complete' as const }]
  }
  return []
}

async function runAction(
  appointment: AdminAppointment,
  action: 'arrive' | 'start' | 'complete' | 'cancel',
) {
  const actionLabels = {
    arrive: '到店',
    start: '开始服务',
    complete: '完成服务',
    cancel: '取消预约',
  }
  try {
    await ElMessageBox.confirm(`确认将预约 #${appointment.id} 标记为${actionLabels[action]}？`, '预约状态变更')
  } catch {
    return
  }
  actionLoadingId.value = appointment.id
  try {
    if (action === 'arrive') {
      await adminApi.arriveAppointment(appointment.id)
    } else if (action === 'start') {
      await adminApi.startAppointment(appointment.id)
    } else if (action === 'complete') {
      await adminApi.completeAppointment(appointment.id)
    } else {
      await adminApi.cancelAppointment(appointment.id)
    }
    ElMessage.success('预约状态已更新')
    await loadAppointments()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '状态更新失败')
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(loadAppointments)
</script>

<template>
  <section class="page-panel">
    <div class="page-heading">
      <div>
        <p class="page-kicker">预约订单</p>
        <h2>订单列表</h2>
      </div>
      <div class="page-actions">
        <el-select v-model="statusFilter" class="status-filter" placeholder="状态">
          <el-option
            v-for="option in statusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          @change="loadAppointments"
        />
        <el-button :loading="loading" @click="loadAppointments">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" class="page-alert" type="error" :title="error" show-icon />

    <el-table v-loading="loading" :data="filteredRows" border stripe>
      <el-table-column prop="id" label="预约 ID" width="100" fixed />
      <el-table-column prop="contactName" label="联系人" min-width="110" />
      <el-table-column prop="contactPhone" label="手机号" min-width="130" />
      <el-table-column prop="serviceName" label="项目" min-width="160" />
      <el-table-column prop="therapistName" label="技师" min-width="110" />
      <el-table-column label="时间" min-width="160">
        <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
      </el-table-column>
      <el-table-column prop="paidAmount" label="实付" width="110">
        <template #default="{ row }">{{ money(row.paidAmount) }}</template>
      </el-table-column>
      <el-table-column prop="paymentStatus" label="支付" width="100" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabels[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="userNote" label="用户备注" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button
            v-for="action in actionOptions(row)"
            :key="action.action"
            size="small"
            :type="action.type"
            :loading="actionLoadingId === row.id"
            @click="runAction(row, action.action)"
          >
            {{ action.label }}
          </el-button>
          <span v-if="!actionOptions(row).length" class="table-muted">无需操作</span>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
