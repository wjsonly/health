function request(path, options = {}) {
  const app = getApp()
  const token = getToken()
  const header = {
    ...(options.header || {})
  }
  if (token) {
    header.Authorization = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBaseUrl}${path}`,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      success: (res) => {
        const body = res.data
        if (body && body.success) {
          resolve(body.data)
          return
        }
        reject(new Error((body && body.message) || '请求失败'))
      },
      fail: reject
    })
  })
}

function getToken() {
  return wx.getStorageSync('mpToken') || ''
}

function getStoredUser() {
  return wx.getStorageSync('mpUser') || null
}

function setSession(session) {
  wx.setStorageSync('mpToken', session.token)
  wx.setStorageSync('mpTokenExpiresAt', session.expiresAt)
  wx.setStorageSync('mpUser', session.user)
}

function clearSession() {
  wx.removeStorageSync('mpToken')
  wx.removeStorageSync('mpTokenExpiresAt')
  wx.removeStorageSync('mpUser')
}

function assetUrl(path) {
  if (!path || /^https?:\/\//i.test(path)) return path || ''
  return `${getApp().globalData.apiBaseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
}

module.exports = {
  assetUrl,
  getToken,
  getStoredUser,
  clearSession,
  loginWithWechatPhone: async (data) => {
    const session = await request('/api/mp/auth/login', { method: 'POST', data })
    setSession(session)
    return session
  },
  getStore: () => request('/api/store'),
  getServiceItems: () => request('/api/service-items'),
  getServiceItem: (id) => request(`/api/service-items/${id}`),
  getTherapists: () => request('/api/therapists?storeId=1'),
  getTherapist: (id) => request(`/api/therapists/${id}`),
  getSlots: (therapistId, serviceItemId, date) => request(`/api/appointments/available-slots?therapistId=${therapistId}&serviceItemId=${serviceItemId}&date=${date}`),
  createAppointment: (data) => request('/api/appointments', { method: 'POST', data }),
  getOrders: () => request('/api/appointments'),
  getOrder: (id) => request(`/api/appointments/${id}`),
  cancelOrder: (id) => request(`/api/appointments/${id}/cancel`, { method: 'PATCH' })
}
