export interface StoredAdmin {
  id: number
  username: string
  displayName: string
}

export const ADMIN_TOKEN_KEY = 'health_admin_token'
export const ADMIN_USER_KEY = 'health_admin_user'

export function getAdminToken() {
  return window.localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

export function getStoredAdmin(): StoredAdmin | null {
  const value = window.localStorage.getItem(ADMIN_USER_KEY)
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as StoredAdmin
  } catch {
    return null
  }
}

export function setAdminSession(token: string, admin: StoredAdmin) {
  window.localStorage.setItem(ADMIN_TOKEN_KEY, token)
  window.localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(admin))
}

export function clearAdminSession() {
  window.localStorage.removeItem(ADMIN_TOKEN_KEY)
  window.localStorage.removeItem(ADMIN_USER_KEY)
}

export function isAdminAuthenticated() {
  return Boolean(getAdminToken())
}
