const api = require('../../utils/api')
const display = require('../../utils/display')

function showError(error) {
  wx.showToast({
    title: error.message || '请求失败',
    icon: 'none'
  })
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (result) => {
        if (result.code) {
          resolve(result.code)
          return
        }
        reject(new Error('微信登录失败'))
      },
      fail: reject
    })
  })
}

Page({
  data: {
    loading: true,
    slotLoading: false,
    submitting: false,
    serviceItems: [],
    therapists: [],
    serviceItemId: '',
    therapistId: '',
    serviceIndex: -1,
    therapistIndex: -1,
    selectedService: null,
    selectedTherapist: null,
    selectedServiceName: '请选择项目',
    selectedTherapistName: '请选择技师',
    date: '',
    minDate: '',
    slots: [],
    selectedSlotIndex: -1,
    selectedSlot: null,
    summaryRows: [],
    canSubmit: false,
    slotEmptyText: '请先选择服务项目和技师',
    submitText: '提交预约',
    authLoading: false,
    wechatAuthorized: false,
    authPhone: '',
    contactName: '',
    contactPhone: '',
    userNote: ''
  },

  onLoad(options) {
    const today = formatDate(new Date())
    this.setData({
      serviceItemId: options.serviceItemId || '',
      therapistId: options.therapistId || '',
      date: today,
      minDate: today
    })
    this.initAuthState()
    this.loadOptions()
  },

  initAuthState() {
    const user = api.getStoredUser()
    if (user && user.phone) {
      this.setData({
        wechatAuthorized: true,
        authPhone: user.phone,
        contactPhone: this.data.contactPhone || user.phone
      }, () => this.updateSummary())
    }
  },

  async onWechatPhoneAuth(event) {
    if (!event.detail || !event.detail.code) {
      wx.showToast({ title: '未完成手机号授权', icon: 'none' })
      return
    }
    this.setData({ authLoading: true })
    try {
      const loginCode = await wxLogin()
      const session = await api.loginWithWechatPhone({
        loginCode,
        phoneCode: event.detail.code
      })
      const phone = session.user && session.user.phone ? session.user.phone : ''
      this.setData({
        authLoading: false,
        wechatAuthorized: true,
        authPhone: phone,
        contactPhone: this.data.contactPhone || phone
      }, () => this.updateSummary())
      wx.showToast({ title: '授权成功', icon: 'success' })
    } catch (error) {
      this.setData({ authLoading: false })
      showError(error)
    }
  },

  async loadOptions() {
    this.setData({ loading: true })
    try {
      const [rawServiceItems, rawTherapists] = await Promise.all([
        api.getServiceItems(),
        api.getTherapists()
      ])
      const serviceItems = rawServiceItems.map(display.shapeService)
      const therapists = rawTherapists.map(display.shapeTherapist)
      let serviceIndex = serviceItems.findIndex((item) => String(item.id) === String(this.data.serviceItemId))
      let therapistIndex = therapists.findIndex((item) => String(item.id) === String(this.data.therapistId))

      if (serviceIndex < 0 && serviceItems.length) {
        serviceIndex = 0
      }
      if (therapistIndex < 0 && therapists.length) {
        therapistIndex = 0
      }

      const selectedService = serviceIndex >= 0 ? serviceItems[serviceIndex] : null
      const selectedTherapist = therapistIndex >= 0 ? therapists[therapistIndex] : null

      this.setData({
        serviceItems,
        therapists,
        serviceItemId: selectedService ? selectedService.id : '',
        therapistId: selectedTherapist ? selectedTherapist.id : '',
        serviceIndex,
        therapistIndex,
        selectedService,
        selectedTherapist,
        selectedServiceName: selectedService ? selectedService.name : '请选择项目',
        selectedTherapistName: selectedTherapist ? selectedTherapist.name : '请选择技师',
        loading: false
      }, () => {
        this.updateSummary()
        this.maybeLoadSlots()
      })
    } catch (error) {
      this.setData({ loading: false })
      showError(error)
    }
  },

  maybeLoadSlots() {
    if (!this.data.serviceItemId) {
      this.setData({
        slots: [],
        selectedSlotIndex: -1,
        selectedSlot: null,
        slotEmptyText: '请先选择服务项目'
      }, () => this.updateSummary())
      return
    }
    if (!this.data.therapistId) {
      this.setData({
        slots: [],
        selectedSlotIndex: -1,
        selectedSlot: null,
        slotEmptyText: '请先选择服务技师'
      }, () => this.updateSummary())
      return
    }
    this.loadSlots()
  },

  async loadSlots() {
    this.setData({ slotLoading: true, slots: [], selectedSlotIndex: -1, selectedSlot: null })
    try {
      const slots = await api.getSlots(this.data.therapistId, this.data.serviceItemId, this.data.date)
      this.setData({
        slots: slots.map((slot) => ({ ...slot, label: display.timeRange(slot.startTime, slot.endTime), activeClass: '' })),
        slotLoading: false,
        slotEmptyText: slots.length ? '' : '当前日期暂无可约时段，可切换其他日期'
      }, () => this.updateSummary())
    } catch (error) {
      this.setData({
        slotLoading: false,
        slotEmptyText: '可约时段加载失败，请稍后重试'
      }, () => this.updateSummary())
      showError(error)
    }
  },

  onServiceChange(event) {
    const serviceIndex = Number(event.detail.value)
    const selectedService = this.data.serviceItems[serviceIndex]
    this.setData({
      serviceIndex,
      serviceItemId: selectedService.id,
      selectedService,
      selectedServiceName: selectedService.name
    }, () => this.maybeLoadSlots())
  },

  onTherapistChange(event) {
    const therapistIndex = Number(event.detail.value)
    const selectedTherapist = this.data.therapists[therapistIndex]
    this.setData({
      therapistIndex,
      therapistId: selectedTherapist.id,
      selectedTherapist,
      selectedTherapistName: selectedTherapist.name
    }, () => this.maybeLoadSlots())
  },

  onDateChange(event) {
    this.setData({ date: event.detail.value }, () => this.maybeLoadSlots())
  },

  selectSlot(event) {
    const selectedSlotIndex = Number(event.currentTarget.dataset.index)
    const selectedSlot = this.data.slots[selectedSlotIndex]
    this.setData({
      selectedSlotIndex,
      selectedSlot,
      slots: this.data.slots.map((slot, index) => ({
        ...slot,
        activeClass: index === selectedSlotIndex ? 'slot-active' : ''
      }))
    }, () => this.updateSummary())
  },

  onContactNameInput(event) {
    this.setData({ contactName: event.detail.value }, () => this.updateSummary())
  },

  onContactPhoneInput(event) {
    this.setData({ contactPhone: event.detail.value }, () => this.updateSummary())
  },

  onNoteInput(event) {
    this.setData({ userNote: event.detail.value })
  },

  updateSummary() {
    const selectedService = this.data.serviceItems[this.data.serviceIndex] || this.data.selectedService
    const selectedTherapist = this.data.therapists[this.data.therapistIndex] || this.data.selectedTherapist
    const selectedSlot = this.data.selectedSlot
    this.setData({
      selectedService,
      selectedTherapist,
      summaryRows: [
        { label: '项目', value: selectedService ? selectedService.name : '待选择' },
        { label: '技师', value: selectedTherapist ? selectedTherapist.name : '待选择' },
        { label: '日期', value: this.data.date || '待选择' },
        { label: '时间', value: selectedSlot ? selectedSlot.label : '待选择' },
        { label: '价格', value: selectedService ? selectedService.priceText : '待确认' }
      ],
      canSubmit: Boolean(
        this.data.wechatAuthorized &&
        this.data.serviceItemId &&
        this.data.therapistId &&
        selectedSlot &&
        this.data.contactName &&
        this.data.contactPhone
      )
    })
  },

  async submitBooking() {
    if (!this.data.serviceItemId) {
      wx.showToast({ title: '请选择服务项目', icon: 'none' })
      return
    }
    if (!this.data.therapistId) {
      wx.showToast({ title: '请选择技师', icon: 'none' })
      return
    }
    if (!this.data.selectedSlot) {
      wx.showToast({ title: '请选择预约时间', icon: 'none' })
      return
    }
    // 个人主体小程序暂不支持手机号快速验证能力，先允许填写手机号后直接预约。
    // if (!this.data.wechatAuthorized || !api.getToken()) {
    //   wx.showToast({ title: '请先授权微信手机号', icon: 'none' })
    //   return
    // }
    if (!this.data.contactName || !this.data.contactPhone) {
      wx.showToast({ title: '请填写联系人和手机号', icon: 'none' })
      return
    }

    this.setData({ submitting: true, submitText: '提交中...' })
    try {
      await api.createAppointment({
        storeId: 1,
        serviceItemId: Number(this.data.serviceItemId),
        therapistId: Number(this.data.therapistId),
        appointmentDate: this.data.date,
        startTime: this.data.selectedSlot.startTime,
        contactName: this.data.contactName,
        contactPhone: this.data.contactPhone,
        userNote: this.data.userNote
      })
      this.setData({ submitting: false, submitText: '提交预约' })
      wx.showToast({ title: '预约成功', icon: 'success' })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/home/index' })
      }, 800)
    } catch (error) {
      this.setData({ submitting: false, submitText: '提交预约' })
      showError(error)
    }
  }
})
