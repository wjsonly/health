const api = require('../../utils/api')
const display = require('../../utils/display')

function showError(error) {
  wx.showToast({
    title: error.message || '请求失败',
    icon: 'none'
  })
}

Page({
  data: {
    id: '',
    loading: true,
    order: null,
    service: null,
    therapist: null,
    store: null,
    steps: [],
    canCancel: false,
    canBookAgain: false
  },

  onLoad(options) {
    this.setData({ id: options.id })
  },

  onShow() {
    if (this.data.id) {
      this.loadOrder()
    }
  },

  async loadOrder() {
    this.setData({ loading: true })
    try {
      const [order, services, therapists, store] = await Promise.all([
        api.getOrder(this.data.id),
        api.getServiceItems(),
        api.getTherapists(),
        api.getStore()
      ])
      const service = services.find((item) => item.id === order.serviceItemId) || { name: '预约项目' }
      const therapist = therapists.find((item) => item.id === order.therapistId) || { name: '到店安排' }
      this.setData({
        order: {
          ...order,
          statusText: display.statusText(order.status),
          timeText: `${order.appointmentDate} ${display.timeRange(order.startTime, order.endTime)}`,
          timeRangeText: display.timeRange(order.startTime, order.endTime),
          priceText: display.money(order.itemAmount),
          userNoteText: order.userNote || '无'
        },
        service,
        therapist,
        store,
        steps: display.progressSteps(order.status),
        canCancel: order.status === 'BOOKED',
        canBookAgain: ['COMPLETED', 'CANCELLED'].includes(order.status),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  cancelOrder() {
    wx.showModal({
      title: '取消预约',
      content: '确认取消当前预约吗？',
      confirmText: '取消预约',
      success: async (result) => {
        if (!result.confirm) {
          return
        }
        try {
          await api.cancelOrder(this.data.id)
          wx.showToast({ title: '已取消', icon: 'success' })
          this.loadOrder()
        } catch (error) {
          showError(error)
        }
      }
    })
  },

  callStore() {
    if (!this.data.store || !this.data.store.phone) {
      wx.showToast({ title: '暂无门店电话', icon: 'none' })
      return
    }
    wx.makePhoneCall({ phoneNumber: this.data.store.phone })
  },

  bookAgain() {
    wx.navigateTo({
      url: `/pages/booking/index?serviceItemId=${this.data.order.serviceItemId}&therapistId=${this.data.order.therapistId}`
    })
  }
})
