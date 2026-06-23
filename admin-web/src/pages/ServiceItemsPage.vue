<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'

import {
  adminApi,
  type ServiceCategory,
  type ServiceCategoryRequest,
  type ServiceItem,
  type ServiceItemRequest,
} from '../api/admin'
import { assetUrl } from '../api/http'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const serviceItems = ref<ServiceItem[]>([])
const categories = ref<ServiceCategory[]>([])
const itemDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const editingItem = ref<ServiceItem | null>(null)
const editingCategory = ref<ServiceCategory | null>(null)

const emptyItemForm = (): ServiceItemRequest => ({
  categoryId: 0,
  name: '',
  imageUrl: '',
  durationMinutes: 60,
  originalPrice: 0,
  salePrice: 0,
  suitablePeople: '',
  highlights: '',
  notice: '',
  hot: false,
  recommended: false,
  status: 'ACTIVE',
  sortOrder: 0,
})

const itemForm = reactive<ServiceItemRequest>(emptyItemForm())
const categoryForm = reactive<ServiceCategoryRequest>({ name: '', sortOrder: 0, enabled: true })
const enabledCategories = computed(() => categories.value.filter((category) => category.enabled))
const itemDialogTitle = computed(() => editingItem.value ? '编辑服务项目' : '新增服务项目')

function money(value: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function statusLabel(status: string) {
  return status === 'ACTIVE' ? '上架' : '下架'
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [items, categoryList] = await Promise.all([
      adminApi.getServiceItems(),
      adminApi.getServiceCategories(),
    ])
    serviceItems.value = items
    categories.value = categoryList
  } catch (err) {
    error.value = err instanceof Error ? err.message : '服务项目加载失败'
  } finally {
    loading.value = false
  }
}

function openCreateItem() {
  editingItem.value = null
  Object.assign(itemForm, emptyItemForm(), { categoryId: enabledCategories.value[0]?.id || 0 })
  itemDialogVisible.value = true
}

function openEditItem(item: ServiceItem) {
  editingItem.value = item
  Object.assign(itemForm, {
    categoryId: item.categoryId,
    name: item.name,
    imageUrl: item.imageUrl || '',
    durationMinutes: item.durationMinutes,
    originalPrice: Number(item.originalPrice),
    salePrice: Number(item.salePrice),
    suitablePeople: item.suitablePeople || '',
    highlights: item.highlights || '',
    notice: item.notice || '',
    hot: item.hot,
    recommended: item.recommended,
    status: item.status,
    sortOrder: item.sortOrder,
  })
  itemDialogVisible.value = true
}

async function saveItem() {
  if (!itemForm.categoryId || !itemForm.name.trim()) {
    ElMessage.warning('请选择分类并填写项目名称')
    return
  }
  saving.value = true
  try {
    const payload = { ...itemForm, name: itemForm.name.trim() }
    if (editingItem.value) {
      await adminApi.updateServiceItem(editingItem.value.id, payload)
    } else {
      await adminApi.createServiceItem(payload)
    }
    ElMessage.success('服务项目已保存')
    itemDialogVisible.value = false
    await loadData()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '服务项目保存失败')
  } finally {
    saving.value = false
  }
}

async function changeItemStatus(item: ServiceItem) {
  const nextStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(`确认${nextStatus === 'ACTIVE' ? '上架' : '下架'}“${item.name}”？`, '状态确认')
    await adminApi.changeServiceItemStatus(item.id, nextStatus)
    ElMessage.success('项目状态已更新')
    await loadData()
  } catch (err) {
    if (err instanceof Error) ElMessage.error(err.message)
  }
}

async function uploadImage(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const result = await adminApi.uploadImage(options.file)
    itemForm.imageUrl = result.url
    options.onSuccess(result)
    ElMessage.success('图片上传成功')
  } catch (err) {
    const message = err instanceof Error ? err.message : '图片上传失败'
    const uploadError = Object.assign(new Error(message), {
      name: 'UploadAjaxError',
      status: 400,
      method: 'POST',
      url: '/api/admin/uploads/images',
    })
    options.onError(uploadError)
    ElMessage.error(message)
  } finally {
    uploading.value = false
  }
}

function resetCategoryForm() {
  editingCategory.value = null
  Object.assign(categoryForm, { name: '', sortOrder: 0, enabled: true })
}

function editCategory(category: ServiceCategory) {
  editingCategory.value = category
  Object.assign(categoryForm, {
    name: category.name,
    sortOrder: category.sortOrder,
    enabled: category.enabled,
  })
}

async function saveCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请填写分类名称')
    return
  }
  saving.value = true
  try {
    const payload = { ...categoryForm, name: categoryForm.name.trim() }
    if (editingCategory.value) {
      await adminApi.updateServiceCategory(editingCategory.value.id, payload)
    } else {
      await adminApi.createServiceCategory(payload)
    }
    ElMessage.success('分类已保存')
    resetCategoryForm()
    await loadData()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '分类保存失败')
  } finally {
    saving.value = false
  }
}

async function changeCategoryStatus(category: ServiceCategory) {
  try {
    await adminApi.changeServiceCategoryStatus(category.id, !category.enabled)
    ElMessage.success('分类状态已更新')
    await loadData()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '分类状态更新失败')
  }
}

onMounted(loadData)
</script>

<template>
  <section class="page-panel">
    <div class="page-heading">
      <div>
        <p class="page-kicker">项目管理</p>
        <h2>服务项目</h2>
      </div>
      <div class="page-actions">
        <el-button @click="categoryDialogVisible = true">分类管理</el-button>
        <el-button type="primary" @click="openCreateItem">新增项目</el-button>
        <el-button :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="error" class="page-alert" type="error" :title="error" show-icon />

    <el-table v-loading="loading" :data="serviceItems" border stripe>
      <el-table-column prop="name" label="项目名称" min-width="180" />
      <el-table-column prop="categoryName" label="分类" min-width="120" />
      <el-table-column prop="durationMinutes" label="时长" width="100">
        <template #default="{ row }">{{ row.durationMinutes }} 分钟</template>
      </el-table-column>
      <el-table-column label="价格" width="150">
        <template #default="{ row }">{{ money(row.salePrice) }} / {{ money(row.originalPrice) }}</template>
      </el-table-column>
      <el-table-column label="标记" width="130">
        <template #default="{ row }">
          <el-tag v-if="row.hot" size="small" type="danger">热门</el-tag>
          <el-tag v-if="row.recommended" class="tag-gap" size="small">推荐</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditItem(row)">编辑</el-button>
          <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="changeItemStatus(row)">
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="itemDialogVisible" :title="itemDialogTitle" width="760px">
      <el-form :model="itemForm" label-width="96px">
        <div class="form-grid">
          <el-form-item label="项目分类" required>
            <el-select v-model="itemForm.categoryId" filterable>
              <el-option v-for="category in enabledCategories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="项目名称" required><el-input v-model="itemForm.name" maxlength="80" /></el-form-item>
          <el-form-item label="服务时长" required><el-input-number v-model="itemForm.durationMinutes" :min="1" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" /></el-form-item>
          <el-form-item label="原价" required><el-input-number v-model="itemForm.originalPrice" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="售价" required><el-input-number v-model="itemForm.salePrice" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="itemForm.status">
              <el-option label="上架" value="ACTIVE" /><el-option label="下架" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item label="项目标记">
            <el-checkbox v-model="itemForm.hot">热门</el-checkbox>
            <el-checkbox v-model="itemForm.recommended">推荐</el-checkbox>
          </el-form-item>
        </div>
        <el-form-item label="项目图片">
          <div class="image-field">
            <img v-if="itemForm.imageUrl" :src="assetUrl(itemForm.imageUrl)" alt="项目图片预览" />
            <el-upload :show-file-list="false" :http-request="uploadImage" accept="image/jpeg,image/png,image/webp">
              <el-button :loading="uploading">上传图片</el-button>
            </el-upload>
            <span class="form-tip">JPEG、PNG 或 WebP，最大 5MB</span>
          </div>
        </el-form-item>
        <el-form-item label="适合人群"><el-input v-model="itemForm.suitablePeople" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="服务亮点"><el-input v-model="itemForm.highlights" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="每行一条亮点" /></el-form-item>
        <el-form-item label="预约须知"><el-input v-model="itemForm.notice" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="itemDialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveItem">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" title="服务分类管理" width="680px" @closed="resetCategoryForm">
      <el-form class="category-form" :model="categoryForm" inline>
        <el-form-item label="名称"><el-input v-model="categoryForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="saveCategory">{{ editingCategory ? '保存修改' : '新增分类' }}</el-button><el-button v-if="editingCategory" @click="resetCategoryForm">取消编辑</el-button></el-form-item>
      </el-form>
      <el-table :data="categories" border>
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="80"><template #default="{ row }">{{ row.enabled ? '启用' : '停用' }}</template></el-table-column>
        <el-table-column label="操作" width="170"><template #default="{ row }"><el-button link type="primary" @click="editCategory(row)">编辑</el-button><el-button link :type="row.enabled ? 'warning' : 'success'" @click="changeCategoryStatus(row)">{{ row.enabled ? '停用' : '启用' }}</el-button></template></el-table-column>
      </el-table>
    </el-dialog>
  </section>
</template>

<style scoped>
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.form-grid :deep(.el-select), .form-grid :deep(.el-input-number) { width: 100%; }
.tag-gap { margin-left: 4px; }
.image-field { display: flex; align-items: center; gap: 12px; }
.image-field img { width: 120px; height: 80px; border: 1px solid #dfe5ec; border-radius: 6px; object-fit: cover; }
.form-tip { color: #8a96a3; font-size: 12px; }
.category-form { margin-bottom: 14px; }
</style>
