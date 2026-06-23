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
    service: null,
    serviceHighlights: [],
    guarantees: ['明码标价', '到店服务', '预约可取消', '专业技师服务']
  },

  onLoad(options) {
    this.loadService(options.id)
  },

  async loadService(id) {
    this.setData({ loading: true })
    try {
      const service = await api.getServiceItem(id)
      const shapedService = {
        ...display.shapeService(service),
        displayImageUrl: api.assetUrl(service.imageUrl)
      }
      this.setData({
        service: shapedService,
        serviceHighlights: (shapedService.highlights || '')
          .split(/\r?\n/)
          .map((item) => item.trim())
          .filter(Boolean),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  bookNow() {
    wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${this.data.service.id}` })
  }
})
