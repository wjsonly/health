<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  adminApi,
  type AdminTherapist,
  type Schedule,
  type ScheduleBatchCreateRequest,
  type ScheduleCreateRequest,
} from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const batchSaving = ref(false)
const error = ref('')
const selectedDate = ref(new Date().toLocaleDateString('en-CA'))
const schedules = ref<Schedule[]>([])
const therapists = ref<AdminTherapist[]>([])

const typeOptions = [
  { label: '上班', value: 'WORK' },
  { label: '休息', value: 'REST' },
  { label: '请假', value: 'LEAVE' },
  { label: '占用', value: 'BLOCKED' },
]

const weekdayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]

const form = reactive({
  therapistId: undefined as number | undefined,
  storeId: 1,
  type: 'WORK',
  startTime: '10:00:00',
  endTime: '18:00:00',
  note: '',
})

const batchForm = reactive({
  therapistIds: [] as number[],
  dateRange: [selectedDate.value, selectedDate.value] as [string, string] | [],
  weekdays: [1, 2, 3, 4, 5] as number[],
  type: 'WORK',
  startTime: '10:00:00',
  endTime: '18:00:00',
  note: '',
})

const batchPreviewCount = computed(() => {
  if (batchForm.dateRange.length !== 2 || !batchForm.therapistIds.length || !batchForm.weekdays.length) {
    return 0
  }
  const [startDate, endDate] = batchForm.dateRange
  const start = new Date(`${startDate}T00:00:00`)
  const end = new Date(`${endDate}T00:00:00`)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end < start) {
    return 0
  }
  const weekdays = new Set(batchForm.weekdays)
  let matchedDays = 0
  const cursor = new Date(start)
  while (cursor <= end) {
    const weekday = cursor.getDay() === 0 ? 7 : cursor.getDay()
    if (weekdays.has(weekday)) {
      matchedDays += 1
    }
    cursor.setDate(cursor.getDate() + 1)
  }
  return matchedDays * batchForm.therapistIds.length
})

function typeLabel(type: string) {
  return typeOptions.find((option) => option.value === type)?.label || type
}

function normalizeTime(value: string | Date) {
  if (value instanceof Date) {
    return value.toTimeString().slice(0, 8)
  }
  return value.length === 5 ? `${value}:00` : value
}

function therapistName(id: number) {
  const therapist = therapists.value.find((item) => item.id === id)
  return therapist ? therapist.name : `#${id}`
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [scheduleList, therapistList] = await Promise.all([
      adminApi.getSchedules(selectedDate.value),
      adminApi.getTherapists(),
    ])
    schedules.value = scheduleList
    therapists.value = therapistList
    if (!form.therapistId && therapistList.length > 0) {
      form.therapistId = therapistList[0].id
      form.storeId = therapistList[0].storeId
    }
    if (!batchForm.therapistIds.length && therapistList.length > 0) {
      batchForm.therapistIds = [therapistList[0].id]
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '排班数据加载失败'
  } finally {
    loading.value = false
  }
}

async function createSchedulesBatch() {
  if (batchForm.dateRange.length !== 2) {
    ElMessage.warning('请选择排班日期范围')
    return
  }
  if (!batchForm.therapistIds.length) {
    ElMessage.warning('请选择技师')
    return
  }
  if (!batchForm.weekdays.length) {
    ElMessage.warning('请选择排班星期')
    return
  }
  batchSaving.value = true
  try {
    const payload: ScheduleBatchCreateRequest = {
      therapistIds: batchForm.therapistIds,
      startDate: batchForm.dateRange[0],
      endDate: batchForm.dateRange[1],
      weekdays: batchForm.weekdays,
      startTime: normalizeTime(batchForm.startTime),
      endTime: normalizeTime(batchForm.endTime),
      type: batchForm.type,
      note: batchForm.note,
    }
    const result = await adminApi.createSchedulesBatch(payload)
    ElMessage.success(`批量排班已创建 ${result.createdCount} 条`)
    await loadData()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '批量创建排班失败')
  } finally {
    batchSaving.value = false
  }
}

function syncStoreId() {
  const therapist = therapists.value.find((item) => item.id === form.therapistId)
  if (therapist) {
    form.storeId = therapist.storeId
  }
}

async function createSchedule() {
  if (!form.therapistId) {
    ElMessage.warning('请选择技师')
    return
  }
  saving.value = true
  try {
    const payload: ScheduleCreateRequest = {
      therapistId: form.therapistId,
      storeId: form.storeId,
      scheduleDate: selectedDate.value,
      startTime: normalizeTime(form.startTime),
      endTime: normalizeTime(form.endTime),
      type: form.type,
      note: form.note,
    }
    await adminApi.createSchedule(payload)
    ElMessage.success('排班已创建')
    await loadData()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '创建排班失败')
  } finally {
    saving.value = false
  }
}

async function deleteSchedule(schedule: Schedule) {
  try {
    await ElMessageBox.confirm(`确认删除 ${schedule.startTime}-${schedule.endTime} 的排班？`, '删除排班')
  } catch {
    return
  }
  try {
    await adminApi.deleteSchedule(schedule.id)
    ElMessage.success('排班已删除')
    await loadData()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '删除排班失败')
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-panel">
    <div class="page-heading">
      <div>
        <p class="page-kicker">排班管理</p>
        <h2>排班计划</h2>
      </div>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <el-alert v-if="error" class="page-alert" type="error" :title="error" show-icon />

    <el-form class="inline-form" :model="form" label-width="76px">
      <el-form-item label="日期">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          @change="loadData"
        />
      </el-form-item>
      <el-form-item label="技师">
        <el-select v-model="form.therapistId" filterable placeholder="选择技师" @change="syncStoreId">
          <el-option
            v-for="therapist in therapists"
            :key="therapist.id"
            :label="therapist.name"
            :value="therapist.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type">
          <el-option v-for="option in typeOptions" :key="option.value" v-bind="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始">
        <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" format="HH:mm" :clearable="false" />
      </el-form-item>
      <el-form-item label="结束">
        <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" format="HH:mm" :clearable="false" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.note" placeholder="选填" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="createSchedule">创建排班</el-button>
      </el-form-item>
    </el-form>

    <div class="section-heading schedule-batch-heading">
      <div>
        <p class="page-kicker">批量排班</p>
        <h3>多日期多技师排班</h3>
      </div>
      <span class="table-muted">预计生成 {{ batchPreviewCount }} 条</span>
    </div>

    <el-form class="inline-form schedule-batch-form" :model="batchForm" label-width="76px">
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="batchForm.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
        />
      </el-form-item>
      <el-form-item label="技师">
        <el-select v-model="batchForm.therapistIds" multiple collapse-tags collapse-tags-tooltip filterable placeholder="选择技师">
          <el-option
            v-for="therapist in therapists"
            :key="therapist.id"
            :label="therapist.name"
            :value="therapist.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="星期">
        <el-checkbox-group v-model="batchForm.weekdays" class="weekday-group">
          <el-checkbox-button v-for="option in weekdayOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </el-checkbox-button>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="batchForm.type">
          <el-option v-for="option in typeOptions" :key="option.value" v-bind="option" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始">
        <el-time-picker v-model="batchForm.startTime" value-format="HH:mm:ss" format="HH:mm" :clearable="false" />
      </el-form-item>
      <el-form-item label="结束">
        <el-time-picker v-model="batchForm.endTime" value-format="HH:mm:ss" format="HH:mm" :clearable="false" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="batchForm.note" placeholder="选填" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="batchSaving" :disabled="batchPreviewCount === 0" @click="createSchedulesBatch">
          批量创建
        </el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="schedules" border stripe>
      <template #empty>
        <el-empty description="当前日期暂无排班，请先为技师创建上班时段。" />
      </template>
      <el-table-column prop="therapistId" label="技师" min-width="140">
        <template #default="{ row }">{{ therapistName(row.therapistId) }}</template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="120" />
      <el-table-column prop="endTime" label="结束时间" width="120" />
      <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" :icon="Delete" @click="deleteSchedule(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
