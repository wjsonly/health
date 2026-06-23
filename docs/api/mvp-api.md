# MVP API

All responses use the same envelope:

```json
{
  "success": true,
  "data": {},
  "message": ""
}
```

When `success` is `false`, `data` is `null` and `message` contains the user-facing error.

## MVP Defaults

- Store ID: `1`
- Mini program demo user ID: `1`
- Mini program API base URL: `http://localhost:8080`
- Admin username: `admin`
- Admin password: `Admin@123456`
- Time format: `HH:mm:ss`
- Date format: `YYYY-MM-DD`

## Public Endpoints

### Store

```text
GET /api/store
```

Returns the first configured store, including `name`, `address`, `phone`, `businessStart`, `businessEnd`, `announcement`, and `status`.

### Service Items

```text
GET /api/service-items
GET /api/service-items/{id}
```

Returns active service items. Item fields include:

```text
id, categoryId, categoryName, name, imageUrl, durationMinutes,
originalPrice, salePrice, suitablePeople, notice, hot, recommended,
status, sortOrder
```

### Therapists

```text
GET /api/therapists?storeId=1
GET /api/therapists/{id}
```

Public therapist responses hide maintenance fields such as phone, employee number, visibility, and certificates. Fields include:

```text
id, name, avatarUrl, gender, yearsOfExperience, level,
introduction, specialties, serviceTags, bookable
```

### Availability

```text
GET /api/appointments/available-slots?therapistId=1&serviceItemId=1&date=2026-06-13
```

Returns available slots:

```json
[
  {
    "startTime": "10:00:00",
    "endTime": "11:00:00"
  }
]
```

Slots are calculated from therapist schedules, service duration, blocking schedules, and blocking appointments.

### Appointments

```text
POST /api/appointments
GET /api/appointments?userId=1
GET /api/appointments/{id}?userId=1
PATCH /api/appointments/{id}/cancel?userId=1
```

Create request:

```json
{
  "userId": 1,
  "storeId": 1,
  "serviceItemId": 1,
  "therapistId": 1,
  "appointmentDate": "2026-06-13",
  "startTime": "10:00:00",
  "contactName": "张三",
  "contactPhone": "13900000000",
  "userNote": "肩颈酸"
}
```

Appointment response fields:

```text
id, userId, storeId, serviceItemId, therapistId, appointmentDate,
startTime, endTime, itemAmount, discountAmount, paidAmount,
paymentStatus, status, contactName, contactPhone, userNote
```

## Admin Endpoints

Admin endpoints under `/api/admin/**` require:

```text
Authorization: Bearer <token>
```

The only public admin endpoint is login:

```text
POST /api/admin/auth/login
```

Login request:

```json
{
  "username": "admin",
  "password": "Admin@123456"
}
```

Login response data:

```json
{
  "token": "jwt-like-token",
  "expiresAt": "2026-06-17T22:00:00Z",
  "admin": {
    "id": 1,
    "username": "admin",
    "displayName": "系统管理员"
  }
}
```

Current admin:

```text
GET /api/admin/auth/me
```

### Therapist Maintenance

```text
GET /api/admin/therapists?storeId=1
POST /api/admin/therapists
PUT /api/admin/therapists/{id}
PATCH /api/admin/therapists/{id}/status
```

Create request:

```json
{
  "storeId": 1,
  "name": "赵敏",
  "avatarUrl": "",
  "gender": "FEMALE",
  "phone": "13800000003",
  "employeeNo": "T003",
  "yearsOfExperience": 5,
  "level": "SENIOR",
  "status": "ACTIVE",
  "introduction": "擅长肩颈放松。",
  "specialties": "肩颈,腰背",
  "serviceTags": "手法稳,亲和",
  "certificateUrls": "",
  "bookable": true,
  "visible": true,
  "sortOrder": 30
}
```

Status request:

```json
{
  "status": "ACTIVE"
}
```

### Schedule Management

```text
GET /api/admin/schedules?date=2026-06-13
POST /api/admin/schedules
DELETE /api/admin/schedules/{id}
```

Create request:

```json
{
  "therapistId": 1,
  "storeId": 1,
  "scheduleDate": "2026-06-13",
  "startTime": "10:00:00",
  "endTime": "18:00:00",
  "type": "WORK",
  "note": "白班"
}
```

Supported schedule types:

```text
WORK, REST, LEAVE, BLOCKED
```

`WORK` adds availability. `REST`, `LEAVE`, and `BLOCKED` remove availability from overlapping work periods.

### Appointment Operations

```text
GET /api/admin/appointments?date=2026-06-13
PATCH /api/admin/appointments/{id}/arrive
PATCH /api/admin/appointments/{id}/start
PATCH /api/admin/appointments/{id}/complete
PATCH /api/admin/appointments/{id}/cancel
```

Status update request:

```json
{
  "status": "ARRIVED",
  "adminNote": "顾客已到店"
}
```

Allowed transitions:

```text
BOOKED -> ARRIVED
BOOKED -> CANCELLED
ARRIVED -> IN_SERVICE
ARRIVED -> CANCELLED
IN_SERVICE -> COMPLETED
```

Invalid transitions return:

```json
{
  "success": false,
  "data": null,
  "message": "当前订单状态不允许此操作"
}
```

Admin appointment responses include public appointment fields plus:

```text
adminNote, createdAt, paidAt, arrivedAt, serviceStartedAt, completedAt, cancelledAt
```
