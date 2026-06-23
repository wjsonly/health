# Health Mini Program

Single-store wellness appointment MVP.

## Modules

- `backend`: Spring Boot REST API.
- `admin-web`: Vue 3 admin operations UI.
- `miniprogram`: native WeChat customer mini program.

## MVP Run Commands

Backend:

```bash
cd backend
mvn test
mvn spring-boot:run
```

The backend uses `backend/src/main/resources/application.yml` for runtime MySQL connection settings. If the configured schema already contains tables but has no `flyway_schema_history`, initialize Flyway history deliberately before running migrations.

## Backend Docker Deployment

The backend can be deployed as a Docker container while connecting to an external MySQL instance. Runtime settings are supplied through environment variables; do not put production passwords in tracked files.

1. Create the local environment file:

   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set the real database and WeChat values:

   ```bash
   SPRING_DATASOURCE_URL=jdbc:mysql://<mysql-host>:<mysql-port>/health?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
   SPRING_DATASOURCE_USERNAME=<mysql-user>
   SPRING_DATASOURCE_PASSWORD=<mysql-password>
   HEALTH_WECHAT_APP_ID=<wechat-app-id>
   HEALTH_WECHAT_APP_SECRET=<wechat-app-secret>
   HEALTH_WECHAT_TOKEN_SECRET=<random-long-token-secret>
   ```

   For local Docker Desktop testing against a MySQL process running on the host machine, keep `host.docker.internal` in `SPRING_DATASOURCE_URL`.

3. Build and start the backend:

   ```bash
   docker compose up -d --build health-backend
   ```

4. Check logs:

   ```bash
   docker compose logs -f health-backend
   ```

5. Stop the backend:

   ```bash
   docker compose down
   ```

Uploaded images are stored in `./data/uploads` on the host and mounted to `/app/data/uploads` in the container.

Admin web:

```bash
cd admin-web
npm install
npm run dev
```

Default admin login:

- Username: `admin`
- Password: `Admin@123456`

Mini program:

Open `miniprogram` in WeChat Developer Tools.

For local development with `http://localhost:8080`, enable the WeChat Developer Tools setting that skips request domain validation, or change `miniprogram/app.js` `apiBaseUrl` to a reachable backend host.

## MVP Smoke Test

1. Start the backend.

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   Expected: Spring Boot starts on `http://localhost:8080`, Flyway applies migrations, and `GET /api/store` returns the seeded store.

2. Start the admin web app.

   ```bash
   cd admin-web
   npm run dev
   ```

   Expected: Vite prints a local URL, and the admin UI can call the backend without browser CORS errors.

3. Verify seeded data in admin.

   Open the Vite URL, log in with `admin` / `Admin@123456`, and check:

   - Service items page shows `肩颈舒缓推拿`, `经典足疗`, and `温阳艾灸调理`.
   - Therapists page shows `李静` and `王明`.

4. Create tomorrow's schedule.

   In admin schedules, create a `WORK` schedule:

   ```json
   {
     "therapistId": 1,
     "storeId": 1,
     "scheduleDate": "2026-06-18",
     "startTime": "10:00:00",
     "endTime": "18:00:00",
     "type": "WORK",
     "note": "白班"
   }
   ```

   Expected: the schedule list shows the new work period. If running after 2026-06-17, replace `2026-06-18` with the next test date.

5. Open the mini program.

   Open `miniprogram` in WeChat Developer Tools.

   Expected: the home page renders store information, trust labels, quick categories, recommended service cards, popular therapist cards, and direct booking actions.

6. Create a customer appointment.

   In the mini program:

   - Tap a quick category such as `肩颈放松`.
   - Open a service item detail, or tap `预约` directly from a service card.
   - Tap `立即预约`.
   - Confirm the booking summary contains the selected service.
   - Select a therapist, tomorrow's date, and an available slot.
   - Enter contact name and phone.
   - Submit the booking.

   Expected: the mini program navigates to the order detail page, shows the status progress, and the order status is `待到店`.

7. Verify the appointment in admin.

   In admin appointments, select the appointment date.

   Expected: the new appointment appears with contact information, service name, therapist name, and status `BOOKED`. The status filter can narrow the list to `已预约`.

8. Move the appointment through fulfillment.

   In admin, run the visible next-step actions in order:

   - 到店
   - 开始
   - 完成

   Expected: status changes from `BOOKED` to `ARRIVED`, then `IN_SERVICE`, then `COMPLETED`.

9. Verify mini program order status.

   Reopen the mini program order detail page.

   Expected: the order reflects the completed status, shows the completed progress state, no longer shows the cancel action, and provides `再次预约`.

## Verification Commands

```bash
cd backend
mvn test
```

Expected: all backend tests pass.

```bash
cd admin-web
npm run build
```

Expected: TypeScript checks and Vite production build pass.

```bash
find miniprogram -name '*.js' -exec node --check {} \;
node -e "const fs=require('fs'); const app=JSON.parse(fs.readFileSync('miniprogram/app.json','utf8')); for (const p of app.pages) { for (const ext of ['js','wxml','wxss','json']) { const f='miniprogram/'+p+'.'+ext; if (!fs.existsSync(f)) throw new Error('missing '+f); } JSON.parse(fs.readFileSync('miniprogram/'+p+'.json','utf8')); } JSON.parse(fs.readFileSync('miniprogram/sitemap.json','utf8')); console.log('mini program static check ok:', app.pages.length, 'pages');"
```

Expected: mini program JavaScript syntax and page file structure checks pass.
