# Experience Optimization V1 Design

## Background

The current project is a functional MVP for a single-store wellness appointment mini program. It supports store display, service items, therapists, schedules, booking, order status transitions, and an admin console. After trying the first phase, the experience feels hard to use because the UI is still organized around raw functions and database-like records instead of the real customer and operator journeys.

Experience Optimization V1 focuses on making the product feel like a practical in-store massage and wellness booking app. The direction is inspired by common local-service patterns: packaged services, clear prices and durations, trust signals, available-time guidance, and fast booking entry points.

## Goal

Make the customer mini program easy enough for a first-time user to complete a booking without explanation, and make the admin console efficient enough for daily appointment operations.

Success means:

- A customer can understand what to book from the home page.
- Service and therapist cards help comparison without opening every detail page.
- The booking flow always shows what has already been selected and what remains.
- Order detail explains the appointment status and next available action.
- Admin users can manage today's appointments without reading IDs or clicking disabled buttons repeatedly.

## Scope

### In Scope

- Customer mini program UX restructuring for home, service list, service detail, therapist list, booking, orders, and order detail.
- Admin console light improvements for dashboard, appointment list, and schedule ergonomics.
- Frontend-only enrichment where existing APIs already provide enough data.
- Backend DTO additions only when needed to remove obvious ID-only displays.
- Static seed data improvements if needed for realistic display text.

### Out of Scope

- Online payment.
- Coupons, membership cards, package cards, stored value.
- Real customer reviews and rating submission.
- Multi-store support.
- Map navigation integration.
- Full CRM or technician payroll/performance management.

## Design Principles

- Customer pages should answer: what is good, who is it for, how long does it take, how much does it cost, and when can I book.
- Prefer direct booking entry points over deep browsing.
- Use service packages as the primary decision unit; therapists support confidence and personalization.
- Keep admin pages operational and compact, not decorative.
- Avoid adding large new backend domains until the booking flow itself feels good.

## Customer Mini Program

### Home

The home page becomes the primary booking conversion page.

Top area:

- Store name, business status, business hours, address, and phone.
- A compact trust strip such as `正规门店`, `明码标价`, `到店服务`, `可取消预约`.
- Primary action: `立即预约`.

Quick categories:

- `肩颈放松`
- `经典足疗`
- `艾灸调理`
- `全部项目`

Recommended services:

- Card shows name, category, duration, sale price, suitable people, and 2-3 tags.
- Card has a direct `预约` action.
- Hot or recommended services get a small visual marker.

Popular therapists:

- Card shows avatar/initial, name, level, years of experience, specialties, and service tags.
- Card includes a simple availability hint such as `今日可约` when available data exists, or `可预约` as a fallback.

### Service List

The service list becomes a package browsing page.

Controls:

- Category segmented tabs: all categories plus known categories from service data.
- Optional quick sort can be deferred; V1 should avoid building complex sorting until real usage exists.

Service card:

- Name, category, duration, price.
- Suitable people and notice preview.
- Tags for hot/recommended/status.
- Two actions: tap card for detail, tap `预约` to jump straight to booking with the service selected.

### Service Detail

The service detail page becomes a decision page, not just a data page.

Sections:

- Hero summary: service name, category, duration, sale price, original price if present.
- `适合人群`: from existing suitable people field.
- `服务亮点`: derived from category, tags, and notice text.
- `预约须知`: existing notice field.
- `门店保障`: fixed copy for V1, such as transparent pricing and scheduled arrival.

Bottom action:

- Fixed bottom bar with price and `立即预约`.

### Therapist List And Detail

Therapist pages should build confidence.

Therapist card:

- Avatar/initial, name, level, years, specialties.
- Tags split from `serviceTags`.
- Status/bookable/visible converted into user-facing availability text.
- Direct `预约TA` action.

Therapist detail:

- Profile summary.
- Specialties and service tags.
- Introduction.
- Direct booking action with therapist selected.

### Booking Flow

The booking page becomes a guided confirmation page.

Structure:

1. Appointment summary card showing selected service, therapist, date, time, and price.
2. Service selector.
3. Therapist selector.
4. Date selector.
5. Time slot selector.
6. Contact form.
7. Sticky submit button.

Behavior:

- If a service is passed in from another page, preselect it.
- If a therapist is passed in from another page, preselect it.
- When service, therapist, or date changes, reload available slots.
- Show empty state text that explains the next step, not only `暂无`.
- Disable submit until service, therapist, date, time, name, and phone are present.

### Orders And Order Detail

Order list:

- Group by current relevance: active orders first, completed/cancelled later.
- Card shows service name, therapist name, appointment date/time, status, and price.
- Primary action based on status: view detail, cancel, book again.

Order detail:

- Status progress: `已预约 -> 已到店 -> 服务中 -> 已完成`.
- Appointment summary with service, therapist, date/time, contact phone, and amount.
- Store contact actions: call store.
- Cancel action only when status allows cancellation.
- `再次预约` action for completed/cancelled orders.

## Admin Console

### Dashboard

Add a stronger daily operations overview:

- Today booked.
- Waiting for arrival.
- In service.
- Completed.
- Cancelled.

Also show a compact list of today's appointments that need action.

### Appointment Page

Improve scanability:

- Replace service item ID and therapist ID with names where available.
- Add status filters: all, booked, arrived, in service, completed, cancelled.
- Show only the valid next action for each row instead of four mostly disabled buttons.
- Keep destructive cancellation as a confirm action.

### Schedule Page

Improve form ergonomics:

- Keep date, therapist, type, and time fields near each other.
- Make schedule type labels human-readable.
- Add clearer empty state and conflict/error messages if backend returns them.

## Data And API Impact

V1 should avoid large schema changes. Existing fields can support most UI improvements:

- Service item: name, categoryName, durationMinutes, originalPrice, salePrice, suitablePeople, notice, hot, recommended, status.
- Therapist: name, gender, yearsOfExperience, level, introduction, specialties, serviceTags, bookable.
- Appointment: serviceItemId, therapistId, appointmentDate, startTime, endTime, status, amount fields, contact fields.

Potential small API improvement:

- Admin appointment responses may include service and therapist display names to avoid lookup logic in the admin table.

Frontend can initially map IDs to names by loading service and therapist lists, as the current dashboard already does.

## Error Handling

- Customer pages show friendly empty states with next-step actions.
- Booking slot load failures show a retry action.
- Booking submit failures keep user input intact.
- Admin action failures show Element Plus error messages and keep the table unchanged until refresh succeeds.
- Unauthorized admin API responses continue redirecting to login.

## Testing And Verification

Backend:

- Run full backend tests if API DTOs change.
- Add focused tests if admin appointment response fields are enriched.

Admin web:

- Run `npm run build`.
- Manually verify login, dashboard, appointment status actions, and schedule creation.

Mini program:

- Run JavaScript syntax and page structure checks.
- Compile in WeChat Developer Tools.
- Smoke test: home -> service detail -> booking -> order detail -> admin status transition -> order detail update.

## Implementation Order

1. Mini program data shaping helpers and shared display constants.
2. Mini program home and card visual refresh.
3. Service list/detail booking entry improvements.
4. Therapist list/detail booking entry improvements.
5. Booking page guided confirmation flow.
6. Orders and order detail status experience.
7. Admin dashboard and appointment scanability.
8. Verification and smoke-test documentation update.

## Open Decisions

- V1 uses simulated trust and popularity labels from existing fields, not real reviews.
- V1 keeps payment as offline/unpaid status.
- V1 keeps single-store assumptions.
- Visual style should feel warm, clean, and service-oriented, but still lightweight enough for a native WeChat mini program.
