# Experience Optimization V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing wellness appointment MVP from functional screens into a smoother in-store massage and wellness booking experience.

**Architecture:** Keep the backend mostly stable and improve the customer mini program first, using small shared display helpers for labels, tags, formatting, and navigation. Improve admin scanability by mapping service and therapist IDs to names on the frontend before considering backend DTO changes.

**Tech Stack:** Native WeChat mini program (`.js`, `.wxml`, `.wxss`), Vue 3 + Element Plus admin web, Spring Boot backend only if a small DTO/test enhancement becomes necessary.

---

## File Structure

### Mini Program

- Create `miniprogram/utils/display.js`
  - Shared labels, status text, price formatting, tag splitting, service/therapist view-model shaping.
- Modify `miniprogram/pages/home/index.js`
- Modify `miniprogram/pages/home/index.wxml`
- Modify `miniprogram/pages/home/index.wxss`
- Modify `miniprogram/pages/service-list/index.js`
- Modify `miniprogram/pages/service-list/index.wxml`
- Modify `miniprogram/pages/service-list/index.wxss`
- Modify `miniprogram/pages/service-detail/index.js`
- Modify `miniprogram/pages/service-detail/index.wxml`
- Modify `miniprogram/pages/service-detail/index.wxss`
- Modify `miniprogram/pages/therapist-list/index.js`
- Modify `miniprogram/pages/therapist-list/index.wxml`
- Modify `miniprogram/pages/therapist-list/index.wxss`
- Modify `miniprogram/pages/therapist-detail/index.js`
- Modify `miniprogram/pages/therapist-detail/index.wxml`
- Modify `miniprogram/pages/therapist-detail/index.wxss`
- Modify `miniprogram/pages/booking/index.js`
- Modify `miniprogram/pages/booking/index.wxml`
- Modify `miniprogram/pages/booking/index.wxss`
- Modify `miniprogram/pages/orders/index.js`
- Modify `miniprogram/pages/orders/index.wxml`
- Modify `miniprogram/pages/orders/index.wxss`
- Modify `miniprogram/pages/order-detail/index.js`
- Modify `miniprogram/pages/order-detail/index.wxml`
- Modify `miniprogram/pages/order-detail/index.wxss`
- Modify `miniprogram/app.wxss`

### Admin Web

- Modify `admin-web/src/pages/DashboardPage.vue`
- Modify `admin-web/src/pages/AppointmentsPage.vue`
- Modify `admin-web/src/pages/SchedulesPage.vue`
- Modify `admin-web/src/styles.css`

### Docs

- Modify `README.md`
- Modify `docs/api/mvp-api.md` only if backend response contracts change.

---

## Task 1: Mini Program Display Helpers

**Files:**
- Create: `miniprogram/utils/display.js`

- [ ] **Step 1: Create shared display helper**

Create `miniprogram/utils/display.js` with this responsibility:

```js
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
```

- [ ] **Step 2: Verify helper syntax**

Run:

```bash
node --check miniprogram/utils/display.js
```

Expected: no output and exit code `0`.

- [ ] **Step 3: Commit**

```bash
git add miniprogram/utils/display.js
git commit -m "feat: add mini program display helpers"
```

---

## Task 2: Home Page Booking Conversion Refresh

**Files:**
- Modify: `miniprogram/pages/home/index.js`
- Modify: `miniprogram/pages/home/index.wxml`
- Modify: `miniprogram/pages/home/index.wxss`
- Modify: `miniprogram/app.wxss`

- [ ] **Step 1: Update home data shaping**

Import `display` helpers and set:

```js
const display = require('../../utils/display')
```

In `data`, add:

```js
quickCategories: [
  { label: '肩颈放松', value: '肩颈' },
  { label: '经典足疗', value: '足疗' },
  { label: '艾灸调理', value: '艾灸' },
  { label: '全部项目', value: '全部' }
],
trustTags: ['正规门店', '明码标价', '到店服务', '可取消预约']
```

When loading services and therapists, shape them:

```js
const shapedServices = serviceItems.map(display.shapeService)
const shapedTherapists = therapists.map(display.shapeTherapist)
const featuredServices = shapedServices.filter((item) => item.hot || item.recommended)
```

Add navigation methods:

```js
goBooking() {
  wx.navigateTo({ url: '/pages/booking/index' })
},

goCategory(event) {
  wx.navigateTo({ url: `/pages/service-list/index?category=${encodeURIComponent(event.currentTarget.dataset.category)}` })
},

bookService(event) {
  wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${event.currentTarget.dataset.id}` })
},

bookTherapist(event) {
  wx.navigateTo({ url: `/pages/booking/index?therapistId=${event.currentTarget.dataset.id}` })
}
```

- [ ] **Step 2: Replace home WXML with conversion layout**

Keep the loading block, then render:

- Store hero with `立即预约`.
- Trust strip from `trustTags`.
- Quick categories.
- Recommended service cards with direct booking button.
- Popular therapist cards with direct booking button.

- [ ] **Step 3: Add home styles**

Add page-local classes:

```css
.hero-actions { margin-top: 24rpx; }
.trust-strip { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 20rpx; }
.trust-pill { background: rgba(255,255,255,0.14); border-radius: 999rpx; color: #fff; font-size: 22rpx; padding: 8rpx 14rpx; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14rpx; }
.quick-item { background: #fff; border: 1rpx solid #e1e7df; border-radius: 8rpx; color: #244333; font-weight: 700; padding: 22rpx 8rpx; text-align: center; }
.card-actions { display: flex; gap: 14rpx; margin-top: 18rpx; }
```

- [ ] **Step 4: Verify**

Run:

```bash
find miniprogram -name '*.js' -exec node --check {} \;
node -e "const fs=require('fs'); const app=JSON.parse(fs.readFileSync('miniprogram/app.json','utf8')); for (const p of app.pages) { for (const ext of ['js','wxml','wxss','json']) { const f='miniprogram/'+p+'.'+ext; if (!fs.existsSync(f)) throw new Error('missing '+f); } JSON.parse(fs.readFileSync('miniprogram/'+p+'.json','utf8')); } JSON.parse(fs.readFileSync('miniprogram/sitemap.json','utf8')); console.log('mini program static check ok:', app.pages.length, 'pages');"
```

Expected: `mini program static check ok: 9 pages`.

- [ ] **Step 5: Commit**

```bash
git add miniprogram/pages/home/index.js miniprogram/pages/home/index.wxml miniprogram/pages/home/index.wxss miniprogram/app.wxss
git commit -m "feat: refresh mini program home experience"
```

---

## Task 3: Service List And Detail Upgrade

**Files:**
- Modify: `miniprogram/pages/service-list/index.js`
- Modify: `miniprogram/pages/service-list/index.wxml`
- Modify: `miniprogram/pages/service-list/index.wxss`
- Modify: `miniprogram/pages/service-detail/index.js`
- Modify: `miniprogram/pages/service-detail/index.wxml`
- Modify: `miniprogram/pages/service-detail/index.wxss`

- [ ] **Step 1: Add service list category state**

In `service-list/index.js`, import display helpers and add data:

```js
const display = require('../../utils/display')

data: {
  loading: true,
  services: [],
  allServices: [],
  categories: ['全部'],
  activeCategory: '全部'
}
```

In `onLoad(options)`, set `activeCategory` from `options.category || '全部'`.

After loading services:

```js
const shapedServices = services.map(display.shapeService)
const categories = ['全部'].concat([...new Set(shapedServices.map((item) => item.categoryKey))].filter((item) => item !== '全部'))
this.setData({
  allServices: shapedServices,
  categories,
  services: filterServices(shapedServices, this.data.activeCategory),
  loading: false
})
```

Add:

```js
function filterServices(services, category) {
  if (!category || category === '全部') return services
  return services.filter((service) => service.categoryKey === category)
}

onCategoryTap(event) {
  const activeCategory = event.currentTarget.dataset.category
  this.setData({
    activeCategory,
    services: filterServices(this.data.allServices, activeCategory)
  })
},

bookService(event) {
  wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${event.currentTarget.dataset.id}` })
}
```

- [ ] **Step 2: Update service list WXML**

Render category segmented controls above the list:

```xml
<view class="category-tabs">
  <view
    wx:for="{{categories}}"
    wx:key="*this"
    class="category-tab {{activeCategory === item ? 'category-tab-active' : ''}}"
    data-category="{{item}}"
    bindtap="onCategoryTap"
  >
    {{item}}
  </view>
</view>
```

Each service card must show name, tags, duration, price, suitable people, and direct `预约`.

- [ ] **Step 3: Update service detail data and actions**

In `service-detail/index.js`, shape the service:

```js
service: display.shapeService(service)
```

Add:

```js
bookService() {
  wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${this.data.service.id}` })
}
```

- [ ] **Step 4: Update service detail WXML**

Render decision sections:

- Hero summary.
- Tags.
- `适合人群`.
- `服务亮点`.
- `预约须知`.
- `门店保障`.
- Sticky bottom action with price and `立即预约`.

- [ ] **Step 5: Add styles and verify**

Run the same mini program static checks from Task 2.

- [ ] **Step 6: Commit**

```bash
git add miniprogram/pages/service-list miniprogram/pages/service-detail
git commit -m "feat: improve service browsing experience"
```

---

## Task 4: Therapist List And Detail Upgrade

**Files:**
- Modify: `miniprogram/pages/therapist-list/index.js`
- Modify: `miniprogram/pages/therapist-list/index.wxml`
- Modify: `miniprogram/pages/therapist-list/index.wxss`
- Modify: `miniprogram/pages/therapist-detail/index.js`
- Modify: `miniprogram/pages/therapist-detail/index.wxml`
- Modify: `miniprogram/pages/therapist-detail/index.wxss`

- [ ] **Step 1: Shape therapist data**

Import `display` helpers in both JS files.

Map API results through:

```js
therapists: therapists.map(display.shapeTherapist)
```

For detail:

```js
therapist: display.shapeTherapist(therapist)
```

- [ ] **Step 2: Add booking navigation**

In list:

```js
bookTherapist(event) {
  wx.navigateTo({ url: `/pages/booking/index?therapistId=${event.currentTarget.dataset.id}` })
}
```

In detail:

```js
bookTherapist() {
  wx.navigateTo({ url: `/pages/booking/index?therapistId=${this.data.therapist.id}` })
}
```

- [ ] **Step 3: Update WXML**

Cards and detail must show:

- Avatar/initial.
- Name.
- Level.
- Years.
- Tags.
- Intro.
- Availability text.
- `预约TA`.

- [ ] **Step 4: Verify and commit**

Run mini program static checks, then:

```bash
git add miniprogram/pages/therapist-list miniprogram/pages/therapist-detail
git commit -m "feat: improve therapist selection experience"
```

---

## Task 5: Guided Booking Page

**Files:**
- Modify: `miniprogram/pages/booking/index.js`
- Modify: `miniprogram/pages/booking/index.wxml`
- Modify: `miniprogram/pages/booking/index.wxss`

- [ ] **Step 1: Add selected service and therapist objects**

In `data`, add:

```js
selectedService: null,
selectedTherapist: null,
summaryRows: [],
canSubmit: false,
slotEmptyText: '请先选择服务项目和技师'
```

After service/therapist/date/slot/contact changes, call:

```js
updateSummary() {
  const selectedService = this.data.serviceItems[this.data.serviceIndex] || null
  const selectedTherapist = this.data.therapists[this.data.therapistIndex] || null
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
    canSubmit: Boolean(this.data.serviceItemId && this.data.therapistId && selectedSlot && this.data.contactName && this.data.contactPhone)
  })
}
```

- [ ] **Step 2: Shape options**

When loading options:

```js
const serviceItems = rawServiceItems.map(display.shapeService)
const therapists = rawTherapists.map(display.shapeTherapist)
```

If a service is not preselected and service items exist, select the first item. Keep the existing behavior of selecting the first therapist.

- [ ] **Step 3: Improve slot empty state**

Set slot empty text:

```js
if (!this.data.serviceItemId) {
  this.setData({ slotEmptyText: '请先选择服务项目' })
  return
}
if (!this.data.therapistId) {
  this.setData({ slotEmptyText: '请先选择服务技师' })
  return
}
```

When no slots after load, show `当前日期暂无可约时段，可切换其他日期`.

- [ ] **Step 4: Update WXML**

Render:

- Summary card.
- Selector card.
- Slot card with explicit empty states.
- Contact card.
- Sticky submit button using `canSubmit`.

- [ ] **Step 5: Verify and commit**

Run mini program checks, then:

```bash
git add miniprogram/pages/booking
git commit -m "feat: guide mini program booking flow"
```

---

## Task 6: Orders And Order Detail Status Experience

**Files:**
- Modify: `miniprogram/pages/orders/index.js`
- Modify: `miniprogram/pages/orders/index.wxml`
- Modify: `miniprogram/pages/orders/index.wxss`
- Modify: `miniprogram/pages/order-detail/index.js`
- Modify: `miniprogram/pages/order-detail/index.wxml`
- Modify: `miniprogram/pages/order-detail/index.wxss`

- [ ] **Step 1: Reuse display helpers in orders**

Replace local status text duplication with:

```js
const display = require('../../utils/display')
```

Shape orders with:

```js
statusText: display.statusText(order.status),
timeText: `${order.appointmentDate} ${display.timeRange(order.startTime, order.endTime)}`,
priceText: display.money(order.itemAmount),
activeOrder: ['BOOKED', 'ARRIVED', 'IN_SERVICE'].includes(order.status)
```

Sort active orders first.

- [ ] **Step 2: Add order actions**

Add:

```js
bookAgain(event) {
  wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${event.currentTarget.dataset.serviceId}&therapistId=${event.currentTarget.dataset.therapistId}` })
}
```

Keep existing detail navigation.

- [ ] **Step 3: Upgrade order detail**

In `order-detail/index.js`, shape:

```js
steps: display.progressSteps(appointment.status),
statusText: display.statusText(appointment.status),
timeText: `${appointment.appointmentDate} ${display.timeRange(appointment.startTime, appointment.endTime)}`,
priceText: display.money(appointment.itemAmount)
```

Add `bookAgain()` and store phone call action when store data is loaded.

- [ ] **Step 4: Update WXML and styles**

Render progress steps, summary rows, cancel only when allowed, and `再次预约` for completed/cancelled.

- [ ] **Step 5: Verify and commit**

Run mini program checks, then:

```bash
git add miniprogram/pages/orders miniprogram/pages/order-detail
git commit -m "feat: improve mini program order status experience"
```

---

## Task 7: Admin Dashboard And Appointment Scanability

**Files:**
- Modify: `admin-web/src/pages/DashboardPage.vue`
- Modify: `admin-web/src/pages/AppointmentsPage.vue`
- Modify: `admin-web/src/styles.css`

- [ ] **Step 1: Add frontend mapping helpers in appointments page**

Load appointments, therapists, and service items together:

```ts
const [appointmentList, therapistList, serviceItemList] = await Promise.all([
  adminApi.getAppointments(selectedDate.value),
  adminApi.getTherapists(),
  adminApi.getServiceItems(),
])
```

Create maps:

```ts
const serviceNameMap = new Map(serviceItemList.map((item) => [item.id, item.name]))
const therapistNameMap = new Map(therapistList.map((item) => [item.id, item.name]))
```

Use computed rows:

```ts
const appointmentRows = computed(() => appointments.value.map((appointment) => ({
  ...appointment,
  serviceName: serviceNameMap.value.get(appointment.serviceItemId) || `项目 #${appointment.serviceItemId}`,
  therapistName: therapistNameMap.value.get(appointment.therapistId) || `技师 #${appointment.therapistId}`,
})))
```

- [ ] **Step 2: Add status filter**

Add:

```ts
const statusFilter = ref('ALL')
const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '已预约', value: 'BOOKED' },
  { label: '已到店', value: 'ARRIVED' },
  { label: '服务中', value: 'IN_SERVICE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]
```

Filter rows before binding to table.

- [ ] **Step 3: Replace disabled button cluster**

Render only valid actions:

- `BOOKED`: `到店`, `取消`
- `ARRIVED`: `开始`, `取消`
- `IN_SERVICE`: `完成`
- other statuses: no primary action

- [ ] **Step 4: Improve dashboard today actions**

Add computed status counts and a small table/list for active appointments.

- [ ] **Step 5: Build and commit**

Run:

```bash
cd admin-web
npm run build
```

Expected: build succeeds; existing Vite chunk warning is acceptable.

Commit:

```bash
git add admin-web/src/pages/DashboardPage.vue admin-web/src/pages/AppointmentsPage.vue admin-web/src/styles.css
git commit -m "feat: improve admin appointment operations"
```

---

## Task 8: Admin Schedule Ergonomics

**Files:**
- Modify: `admin-web/src/pages/SchedulesPage.vue`
- Modify: `admin-web/src/styles.css`

- [ ] **Step 1: Add Chinese schedule type labels**

Use:

```ts
const scheduleTypeLabels: Record<string, string> = {
  WORK: '上班',
  REST: '休息',
  LEAVE: '请假',
  BLOCKED: '占用',
}
```

Render labels in table and select options.

- [ ] **Step 2: Improve empty and error states**

If schedule list is empty, show clear text:

```text
当前日期暂无排班，请先为技师创建上班时段。
```

Keep backend error messages visible through existing `error` state or `ElMessage.error`.

- [ ] **Step 3: Build and commit**

Run:

```bash
cd admin-web
npm run build
```

Commit:

```bash
git add admin-web/src/pages/SchedulesPage.vue admin-web/src/styles.css
git commit -m "feat: improve admin schedule ergonomics"
```

---

## Task 9: Documentation And Full Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update smoke test**

Update README smoke test to include:

- Home category entry.
- Direct service booking.
- Therapist direct booking.
- Guided booking summary.
- Order detail progress.
- Admin status-filtered appointment operations.

- [ ] **Step 2: Run backend tests if backend changed**

If no backend files changed, skip full backend tests and record that backend contracts were unchanged.

If backend files changed, run:

```bash
cd backend
mvn test
```

Expected: all tests pass.

- [ ] **Step 3: Run admin build**

```bash
cd admin-web
npm run build
```

Expected: TypeScript and Vite build pass.

- [ ] **Step 4: Run mini program static checks**

```bash
find miniprogram -name '*.js' -exec node --check {} \;
node -e "const fs=require('fs'); const app=JSON.parse(fs.readFileSync('miniprogram/app.json','utf8')); for (const p of app.pages) { for (const ext of ['js','wxml','wxss','json']) { const f='miniprogram/'+p+'.'+ext; if (!fs.existsSync(f)) throw new Error('missing '+f); } JSON.parse(fs.readFileSync('miniprogram/'+p+'.json','utf8')); } JSON.parse(fs.readFileSync('miniprogram/sitemap.json','utf8')); console.log('mini program static check ok:', app.pages.length, 'pages');"
```

Expected: `mini program static check ok: 9 pages`.

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: update experience v1 smoke test"
```

---

## Self-Review

- Spec coverage: The plan covers mini program home, service list/detail, therapist list/detail, booking, orders/detail, admin dashboard, appointments, schedules, and verification docs.
- Scope control: Payment, coupons, membership, reviews, map navigation, multi-store, and CRM remain out of scope.
- Backend impact: No backend task is required unless admin name mapping cannot be done cleanly on the frontend.
- Testing: Each mini program task includes static checks; admin tasks include production build; backend tests are only required if backend contracts change.
