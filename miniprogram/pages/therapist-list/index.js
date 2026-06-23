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
    therapists: []
  },

  onLoad() {
    this.loadTherapists()
  },

  async loadTherapists() {
    this.setData({ loading: true })
    try {
      const therapists = await api.getTherapists()
      this.setData({
        therapists: therapists.map(display.shapeTherapist),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/therapist-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  bookTherapist(event) {
    const therapist = this.data.therapists.find((item) => String(item.id) === String(event.currentTarget.dataset.id))
    if (!therapist || therapist.bookable === false || therapist.status === 'INACTIVE') {
      wx.showToast({ title: '该技师暂不可约', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/booking/index?therapistId=${therapist.id}` })
  }
})
