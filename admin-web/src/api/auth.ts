import { http } from './http'
import type { StoredAdmin } from '../auth/session'

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginResponse {
  token: string
  expiresAt: string
  admin: StoredAdmin
}

export const authApi = {
  login(payload: AdminLoginRequest) {
    return http.post<AdminLoginResponse>('/api/admin/auth/login', payload)
  },
  me() {
    return http.get<StoredAdmin>('/api/admin/auth/me')
  },
}
