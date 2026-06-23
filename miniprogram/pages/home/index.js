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
    store: null,
    services: [],
    therapists: [],
    quickCategories: [
      { label: '肩颈放松', value: '肩颈' },
      { label: '经典足疗', value: '足疗' },
      { label: '艾灸调理', value: '艾灸' },
      { label: '全部项目', value: '全部' }
    ],
    trustTags: ['正规门店', '明码标价', '到店服务', '可取消预约']
  },

  onLoad() {
    this.loadHome()
  },

  async loadHome() {
    this.setData({ loading: true })
    try {
      const [store, serviceItems, therapists] = await Promise.all([
        api.getStore(),
        api.getServiceItems(),
        api.getTherapists()
      ])
      const shapedServices = serviceItems.map(display.shapeService)
      const shapedTherapists = therapists.map(display.shapeTherapist)
      const featuredServices = shapedServices.filter((item) => item.hot || item.recommended)
      this.setData({
        store,
        services: (featuredServices.length ? featuredServices : shapedServices).slice(0, 3),
        therapists: shapedTherapists.slice(0, 2),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  goServices() {
    wx.navigateTo({ url: '/pages/service-list/index' })
  },

  goBooking() {
    wx.navigateTo({ url: '/pages/booking/index' })
  },

  goCategory(event) {
    const category = encodeURIComponent(event.currentTarget.dataset.category)
    wx.navigateTo({ url: `/pages/service-list/index?category=${category}` })
  },

  goTherapists() {
    wx.navigateTo({ url: '/pages/therapist-list/index' })
  },

  goServiceDetail(event) {
    wx.navigateTo({ url: `/pages/service-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  bookService(event) {
    wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${event.currentTarget.dataset.id}` })
  },

  goTherapistDetail(event) {
    wx.navigateTo({ url: `/pages/therapist-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  bookTherapist(event) {
    wx.navigateTo({ url: `/pages/booking/index?therapistId=${event.currentTarget.dataset.id}` })
  }
})
