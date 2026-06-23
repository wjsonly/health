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
    therapist: null,
    specialties: []
  },

  onLoad(options) {
    this.loadTherapist(options.id)
  },

  async loadTherapist(id) {
    this.setData({ loading: true })
    try {
      const therapist = await api.getTherapist(id)
      const shapedTherapist = display.shapeTherapist(therapist)
      this.setData({
        therapist: shapedTherapist,
        specialties: display.splitTags(therapist.specialties),
        loading: false
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  bookTherapist() {
    if (!this.data.therapist || this.data.therapist.bookable === false || this.data.therapist.status === 'INACTIVE') {
      wx.showToast({ title: '该技师暂不可约', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/booking/index?therapistId=${this.data.therapist.id}` })
  }
})
