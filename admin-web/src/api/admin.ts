import { http } from './http'

export interface ServiceItem {
  id: number
  categoryId: number
  categoryName: string
  name: string
  imageUrl: string
  durationMinutes: number
  originalPrice: number
  salePrice: number
  suitablePeople: string
  highlights: string
  notice: string
  hot: boolean
  recommended: boolean
  status: string
  sortOrder: number
}

export interface ServiceItemRequest {
  categoryId: number
  name: string
  imageUrl?: string
  durationMinutes: number
  originalPrice: number
  salePrice: number
  suitablePeople?: string
  highlights?: string
  notice?: string
  hot: boolean
  recommended: boolean
  status: string
  sortOrder: number
}

export interface ServiceCategory {
  id: number
  name: string
  sortOrder: number
  enabled: boolean
}

export interface ServiceCategoryRequest {
  name: string
  sortOrder: number
  enabled: boolean
}

export interface ImageUploadResponse {
  url: string
}

export interface AdminTherapist {
  id: number
  storeId: number
  name: string
  avatarUrl: string
  gender: string
  phone: string
  employeeNo: string
  yearsOfExperience: number
  level: string
  status: string
  introduction: string
  specialties: string
  serviceTags: string
  certificateUrls: string
  bookable: boolean
  visible: boolean
  sortOrder: number
}

export interface TherapistCreateRequest {
  storeId: number
  name: string
  avatarUrl?: string
  gender: string
  phone: string
  employeeNo: string
  yearsOfExperience: number
  level: string
  status: string
  introduction?: string
  specialties?: string
  serviceTags?: string
  certificateUrls?: string
  bookable: boolean
  visible: boolean
  sortOrder: number
}

export interface TherapistUpdateRequest {
  name: string
  avatarUrl?: string
  gender: string
  phone: string
  yearsOfExperience: number
  level: string
  status: string
  introduction?: string
  specialties?: string
  serviceTags?: string
  certificateUrls?: string
  bookable: boolean
  visible: boolean
  sortOrder: number
}

export interface Schedule {
  id: number
  therapistId: number
  storeId: number
  scheduleDate: string
  startTime: string
  endTime: string
  type: string
  note: string
}

export interface ScheduleCreateRequest {
  therapistId: number
  storeId: number
  scheduleDate: string
  startTime: string
  endTime: string
  type: string
  note?: string
}

export interface ScheduleBatchCreateRequest {
  therapistIds: number[]
  startDate: string
  endDate: string
  weekdays: number[]
  startTime: string
  endTime: string
  type: string
  note?: string
}

export interface ScheduleBatchCreateResponse {
  createdCount: number
  schedules: Schedule[]
}

export interface AdminAppointment {
  id: number
  userId: number
  storeId: number
  serviceItemId: number
  therapistId: number
  appointmentDate: string
  startTime: string
  endTime: string
  itemAmount: number
  discountAmount: number
  paidAmount: number
  paymentStatus: string
  status: string
  contactName: string
  contactPhone: string
  userNote: string
  adminNote: string
  createdAt: string
  paidAt: string | null
  arrivedAt: string | null
  serviceStartedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
}

export type AppointmentActionStatus = 'ARRIVED' | 'IN_SERVICE' | 'COMPLETED' | 'CANCELLED'

interface AppointmentStatusRequest {
  status: AppointmentActionStatus
  adminNote?: string
}

export const adminApi = {
  getServiceItems: () => http.get<ServiceItem[]>('/api/admin/service-items'),
  createServiceItem: (payload: ServiceItemRequest) =>
    http.post<ServiceItem, ServiceItemRequest>('/api/admin/service-items', payload),
  updateServiceItem: (id: number, payload: ServiceItemRequest) =>
    http.put<ServiceItem, ServiceItemRequest>(`/api/admin/service-items/${id}`, payload),
  changeServiceItemStatus: (id: number, status: string) =>
    http.patch<ServiceItem, { status: string }>(`/api/admin/service-items/${id}/status`, { status }),

  getServiceCategories: () => http.get<ServiceCategory[]>('/api/admin/service-categories'),
  createServiceCategory: (payload: ServiceCategoryRequest) =>
    http.post<ServiceCategory, ServiceCategoryRequest>('/api/admin/service-categories', payload),
  updateServiceCategory: (id: number, payload: ServiceCategoryRequest) =>
    http.put<ServiceCategory, ServiceCategoryRequest>(`/api/admin/service-categories/${id}`, payload),
  changeServiceCategoryStatus: (id: number, enabled: boolean) =>
    http.patch<ServiceCategory, { enabled: boolean }>(`/api/admin/service-categories/${id}/status`, { enabled }),
  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post<ImageUploadResponse, FormData>('/api/admin/uploads/images', formData)
  },

  getTherapists: (storeId = 1) =>
    http.get<AdminTherapist[]>('/api/admin/therapists', { params: { storeId } }),
  createTherapist: (payload: TherapistCreateRequest) =>
    http.post<AdminTherapist, TherapistCreateRequest>('/api/admin/therapists', payload),
  updateTherapist: (id: number, payload: TherapistUpdateRequest) =>
    http.put<AdminTherapist, TherapistUpdateRequest>(`/api/admin/therapists/${id}`, payload),
  changeTherapistStatus: (id: number, status: string) =>
    http.patch<AdminTherapist, { status: string }>(`/api/admin/therapists/${id}/status`, { status }),

  getSchedules: (date: string) =>
    http.get<Schedule[]>('/api/admin/schedules', { params: { date } }),
  createSchedule: (payload: ScheduleCreateRequest) =>
    http.post<Schedule, ScheduleCreateRequest>('/api/admin/schedules', payload),
  createSchedulesBatch: (payload: ScheduleBatchCreateRequest) =>
    http.post<ScheduleBatchCreateResponse, ScheduleBatchCreateRequest>('/api/admin/schedules/batch', payload),
  deleteSchedule: (id: number) => http.delete<void>(`/api/admin/schedules/${id}`),

  getAppointments: (date: string) =>
    http.get<AdminAppointment[]>('/api/admin/appointments', { params: { date } }),
  arriveAppointment: (id: number, adminNote?: string) =>
    updateAppointmentStatus(id, 'arrive', { status: 'ARRIVED', adminNote }),
  startAppointment: (id: number, adminNote?: string) =>
    updateAppointmentStatus(id, 'start', { status: 'IN_SERVICE', adminNote }),
  completeAppointment: (id: number, adminNote?: string) =>
    updateAppointmentStatus(id, 'complete', { status: 'COMPLETED', adminNote }),
  cancelAppointment: (id: number, adminNote?: string) =>
    updateAppointmentStatus(id, 'cancel', { status: 'CANCELLED', adminNote }),
}

function updateAppointmentStatus(
  id: number,
  action: 'arrive' | 'start' | 'complete' | 'cancel',
  payload: AppointmentStatusRequest,
) {
  return http.patch<AdminAppointment, AppointmentStatusRequest>(
    `/api/admin/appointments/${id}/${action}`,
    payload,
  )
}
