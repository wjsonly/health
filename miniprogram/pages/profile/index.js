const api = require('../../utils/api')

function showError(error) {
  wx.showToast({
    title: error.message || '加载失败',
    icon: 'none'
  })
}

Page({
  data: {
    user: {
      nickname: '静养会员',
      phone: '未授权'
    },
    store: null
  },

  onLoad() {
    this.loadUser()
    this.loadStore()
  },

  onShow() {
    this.loadUser()
  },

  loadUser() {
    const user = api.getStoredUser()
    if (user && user.phone) {
      this.setData({
        user: {
          nickname: '静养会员',
          phone: user.phone
        }
      })
    }
  },

  async loadStore() {
    try {
      const store = await api.getStore()
      this.setData({ store })
    } catch (error) {
      showError(error)
    }
  },

  goOrders() {
    wx.switchTab({ url: '/pages/orders/index' })
  },

  callStore() {
    if (!this.data.store || !this.data.store.phone) {
      return
    }
    wx.makePhoneCall({ phoneNumber: this.data.store.phone })
  }
})
