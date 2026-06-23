import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
} from 'axios'

import { clearAdminSession, getAdminToken } from '../auth/session'

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

interface ApiClient extends AxiosInstance {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>
  put<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>
  patch<T = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<T>
}

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export function assetUrl(path?: string) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  return `${apiBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
}

const axiosInstance = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
})

axiosInstance.interceptors.request.use((config) => {
  const token = getAdminToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

axiosInstance.interceptors.response.use((response: AxiosResponse<ApiResponse<unknown>>) => {
  const body = response.data as ApiResponse<unknown>
  if (body && body.success === false) {
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body.data as unknown as AxiosResponse<ApiResponse<unknown>>
}, (error) => {
  if (error.response?.status === 401) {
    clearAdminSession()
    const current = `${window.location.pathname}${window.location.search}`
    if (!window.location.pathname.startsWith('/login')) {
      window.location.assign(`/login?redirect=${encodeURIComponent(current)}`)
    }
  }
  const message = error.response?.data?.message || error.message || '请求失败'
  return Promise.reject(new Error(message))
})

export const http = axiosInstance as ApiClient
