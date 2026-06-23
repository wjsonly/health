<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  adminApi,
  type AdminTherapist,
  type TherapistCreateRequest,
  type TherapistUpdateRequest,
} from '../api/admin'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const therapists = ref<AdminTherapist[]>([])
const dialogVisible = ref(false)
const editingTherapist = ref<AdminTherapist | null>(null)

const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
]

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
]

const form = reactive({
  storeId: 1,
  name: '',
  avatarUrl: '',
  gender: 'FEMALE',
  phone: '',
  employeeNo: '',
  yearsOfExperience: 0,
  level: '',
  status: 'ACTIVE',
  introduction: '',
  specialties: '',
  serviceTags: '',
  certificateUrls: '',
  bookable: true,
  visible: true,
  sortOrder: 0,
})

const dialogTitle = computed(() => (editingTherapist.value ? '编辑技师' : '新增技师'))

function genderLabel(gender: string) {
  const labels: Record<string, string> = {
    MALE: '男',
    FEMALE: '女',
  }
  return labels[gender] || gender
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    ACTIVE: '启用',
    INACTIVE: '停用',
  }
  return labels[status] || status
}

function resetForm(therapist?: AdminTherapist) {
  editingTherapist.value = therapist || null
  Object.assign(form, {
    storeId: therapist?.storeId || 1,
    name: therapist?.name || '',
    avatarUrl: therapist?.avatarUrl || '',
    gender: therapist?.gender || 'FEMALE',
    phone: therapist?.phone || '',
    employeeNo: therapist?.employeeNo || '',
    yearsOfExperience: therapist?.yearsOfExperience || 0,
    level: therapist?.level || '',
    status: therapist?.status || 'ACTIVE',
    introduction: therapist?.introduction || '',
    specialties: therapist?.specialties || '',
    serviceTags: therapist?.serviceTags || '',
    certificateUrls: therapist?.certificateUrls || '',
    bookable: therapist?.bookable ?? true,
    visible: therapist?.visible ?? true,
    sortOrder: therapist?.sortOrder || 0,
  })
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(therapist: AdminTherapist) {
  resetForm(therapist)
  dialogVisible.value = true
}

function buildCreatePayload(): TherapistCreateRequest {
  return {
    storeId: form.storeId,
    name: form.name,
    avatarUrl: form.avatarUrl,
    gender: form.gender,
    phone: form.phone,
    employeeNo: form.employeeNo,
    yearsOfExperience: form.yearsOfExperience,
    level: form.level,
    status: form.status,
    introduction: form.introduction,
    specialties: form.specialties,
    serviceTags: form.serviceTags,
    certificateUrls: form.certificateUrls,
    bookable: form.bookable,
    visible: form.visible,
    sortOrder: form.sortOrder,
  }
}

function buildUpdatePayload(): TherapistUpdateRequest {
  return {
    name: form.name,
    avatarUrl: form.avatarUrl,
    gender: form.gender,
    phone: form.phone,
    yearsOfExperience: form.yearsOfExperience,
    level: form.level,
    status: form.status,
    introduction: form.introduction,
    specialties: form.specialties,
    serviceTags: form.serviceTags,
    certificateUrls: form.certificateUrls,
    bookable: form.bookable,
    visible: form.visible,
    sortOrder: form.sortOrder,
  }
}

async function loadTherapists() {
  loading.value = true
  error.value = ''
  try {
    therapists.value = await adminApi.getTherapists()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '技师列表加载失败'
  } finally {
    loading.value = false
  }
}

async function saveTherapist() {
  saving.value = true
  try {
    if (editingTherapist.value) {
      await adminApi.updateTherapist(editingTherapist.value.id, buildUpdatePayload())
      ElMessage.success('技师信息已更新')
    } else {
      await adminApi.createTherapist(buildCreatePayload())
      ElMessage.success('技师已创建')
    }
    dialogVisible.value = false
    await loadTherapists()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(therapist: AdminTherapist) {
  const nextStatus = therapist.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(`确认${nextStatus === 'ACTIVE' ? '启用' : '停用'} ${therapist.name}？`, '状态变更')
  } catch {
    return
  }
  try {
    await adminApi.changeTherapistStatus(therapist.id, nextStatus)
    ElMessage.success('状态已更新')
    await loadTherapists()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '状态更新失败')
  }
}

onMounted(loadTherapists)
</script>

<template>
  <section class="page-panel">
    <div class="page-heading">
      <div>
        <p class="page-kicker">技师管理</p>
        <h2>技师档案</h2>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="loadTherapists">刷新</el-button>
        <el-button type="primary" @click="openCreateDialog">新增技师</el-button>
      </div>
    </div>

    <el-alert v-if="error" class="page-alert" type="error" :title="error" show-icon />

    <el-table v-loading="loading" :data="therapists" border stripe>
      <el-table-column prop="name" label="姓名" min-width="120" fixed />
      <el-table-column prop="gender" label="性别" width="90">
        <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="employeeNo" label="工号" min-width="120" />
      <el-table-column prop="level" label="级别" min-width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bookable" label="可预约" width="90">
        <template #default="{ row }">{{ row.bookable ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="visible" label="可见" width="80">
        <template #default="{ row }">{{ row.visible ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="changeStatus(row)">
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form label-width="96px" :model="form">
        <div class="form-grid">
          <el-form-item label="门店 ID" required>
            <el-input-number v-model="form.storeId" :min="1" :disabled="!!editingTherapist" />
          </el-form-item>
          <el-form-item label="姓名" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="性别" required>
            <el-select v-model="form.gender">
              <el-option v-for="option in genderOptions" :key="option.value" v-bind="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号" required>
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="工号" required>
            <el-input v-model="form.employeeNo" :disabled="!!editingTherapist" />
          </el-form-item>
          <el-form-item label="级别" required>
            <el-input v-model="form.level" />
          </el-form-item>
          <el-form-item label="年限" required>
            <el-input-number v-model="form.yearsOfExperience" :min="0" />
          </el-form-item>
          <el-form-item label="状态" required>
            <el-select v-model="form.status">
              <el-option v-for="option in statusOptions" :key="option.value" v-bind="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" />
          </el-form-item>
          <el-form-item label="头像">
            <el-input v-model="form.avatarUrl" />
          </el-form-item>
          <el-form-item label="可预约">
            <el-switch v-model="form.bookable" />
          </el-form-item>
          <el-form-item label="前台可见">
            <el-switch v-model="form.visible" />
          </el-form-item>
        </div>
        <el-form-item label="简介">
          <el-input v-model="form.introduction" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="擅长">
          <el-input v-model="form.specialties" />
        </el-form-item>
        <el-form-item label="服务标签">
          <el-input v-model="form.serviceTags" />
        </el-form-item>
        <el-form-item label="证书链接">
          <el-input v-model="form.certificateUrls" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTherapist">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>
