const api = require('../../utils/api')
const display = require('../../utils/display')

function showError(error) {
  wx.showToast({
    title: error.message || '加载失败',
    icon: 'none'
  })
}

function filterServices(services, category) {
  if (!category || category === '全部') return services
  return services.filter((service) => service.categoryKey === category)
}

Page({
  data: {
    loading: true,
    services: [],
    allServices: [],
    categories: ['全部'],
    activeCategory: '全部'
  },

  onLoad(options) {
    this.setData({ activeCategory: options.category ? decodeURIComponent(options.category) : '全部' })
    this.loadServices()
  },

  async loadServices() {
    this.setData({ loading: true })
    try {
      const services = await api.getServiceItems()
      const shapedServices = services.map((service) => ({
        ...display.shapeService(service),
        displayImageUrl: api.assetUrl(service.imageUrl)
      }))
      const categories = ['全部'].concat(
        [...new Set(shapedServices.map((item) => item.categoryKey))].filter((item) => item !== '全部')
      )
      this.setData({
        allServices: shapedServices,
        categories,
        services: filterServices(shapedServices, this.data.activeCategory),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  onCategoryTap(event) {
    const activeCategory = event.currentTarget.dataset.category
    this.setData({
      activeCategory,
      services: filterServices(this.data.allServices, activeCategory)
    })
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/service-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  bookService(event) {
    wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${event.currentTarget.dataset.id}` })
  }
})
