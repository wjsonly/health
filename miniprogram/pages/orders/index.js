const api = require('../../utils/api')
const display = require('../../utils/display')

function showError(error) {
  wx.showToast({
    title: error.message || '加载失败',
    icon: 'none'
  })
}

Page({
  data: {
    loading: true,
    orders: []
  },

  onShow() {
    this.loadOrders()
  },

  async loadOrders() {
    this.setData({ loading: true })
    try {
      const [orders, services, therapists] = await Promise.all([
        api.getOrders(),
        api.getServiceItems(),
        api.getTherapists()
      ])
      const serviceMap = services.reduce((map, service) => {
        map[service.id] = service.name
        return map
      }, {})
      const therapistMap = therapists.reduce((map, therapist) => {
        map[therapist.id] = therapist.name
        return map
      }, {})
      const shapedOrders = orders.map((order) => ({
        ...order,
        serviceName: serviceMap[order.serviceItemId] || '预约项目',
        therapistName: therapistMap[order.therapistId] || '到店安排',
        statusText: display.statusText(order.status),
        timeText: `${order.appointmentDate} ${display.timeRange(order.startTime, order.endTime)}`,
        priceText: display.money(order.itemAmount),
        activeOrder: ['BOOKED', 'ARRIVED', 'IN_SERVICE'].includes(order.status),
        canCancel: order.status === 'BOOKED',
        canBookAgain: ['COMPLETED', 'CANCELLED'].includes(order.status)
      })).sort((left, right) => Number(right.activeOrder) - Number(left.activeOrder))
      this.setData({
        orders: shapedOrders,
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/order-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  goBooking() {
    wx.navigateTo({ url: '/pages/service-list/index' })
  },

  bookAgain(event) {
    wx.navigateTo({
      url: `/pages/booking/index?serviceItemId=${event.currentTarget.dataset.serviceId}&therapistId=${event.currentTarget.dataset.therapistId}`
    })
  }
})
