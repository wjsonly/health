const STATUS_TEXT = {
  BOOKED: '待到店',
  PAID: '已支付',
  ARRIVED: '已到店',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const STATUS_STEPS = [
  { status: 'BOOKED', label: '已预约' },
  { status: 'ARRIVED', label: '已到店' },
  { status: 'IN_SERVICE', label: '服务中' },
  { status: 'COMPLETED', label: '已完成' }
]

function money(value) {
  return `¥${Number(value || 0).toFixed(0)}`
}

function timeRange(startTime, endTime) {
  if (!startTime || !endTime) {
    return ''
  }
  return `${startTime.slice(0, 5)}-${endTime.slice(0, 5)}`
}

function splitTags(value) {
  return String(value || '')
    .split(/[,，、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3)
}

function serviceTags(service) {
  const tags = []
  if (service.hot) tags.push('热门')
  if (service.recommended) tags.push('推荐')
  if (service.durationMinutes) tags.push(`${service.durationMinutes}分钟`)
  return tags.slice(0, 3)
}

function categoryKey(name) {
  if (!name) return '全部'
  if (name.includes('肩') || name.includes('颈')) return '肩颈'
  if (name.includes('足')) return '足疗'
  if (name.includes('艾')) return '艾灸'
  return name
}

function shapeService(service) {
  return {
    ...service,
    priceText: money(service.salePrice),
    originalPriceText: service.originalPrice ? money(service.originalPrice) : '',
    durationText: `${service.durationMinutes || 0}分钟`,
    categoryKey: categoryKey(service.categoryName),
    tags: serviceTags(service),
    primaryTag: service.hot ? '热门项目' : service.recommended ? '店长推荐' : '可预约',
    suitableText: service.suitablePeople || '适合日常放松与身体调理'
  }
}

function shapeTherapist(therapist) {
  const tags = splitTags(therapist.serviceTags || therapist.specialties)
  return {
    ...therapist,
    initial: therapist.name ? therapist.name.slice(0, 1) : '技',
    tags,
    yearsText: `${therapist.yearsOfExperience || 0}年经验`,
    availableText: therapist.bookable === false || therapist.status === 'INACTIVE' ? '暂不可约' : '可预约',
    introText: therapist.introduction || '擅长到店养生调理服务'
  }
}

function statusText(status) {
  return STATUS_TEXT[status] || status || '未知状态'
}

function progressSteps(status) {
  if (status === 'CANCELLED') {
    return [{ label: '已取消', active: true }]
  }
  const currentIndex = STATUS_STEPS.findIndex((step) => step.status === status)
  return STATUS_STEPS.map((step, index) => ({
    ...step,
    active: currentIndex >= index,
    current: currentIndex === index
  }))
}

module.exports = {
  categoryKey,
  money,
  progressSteps,
  shapeService,
  shapeTherapist,
  splitTags,
  statusText,
  timeRange
}
