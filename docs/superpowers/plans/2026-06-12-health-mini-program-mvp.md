# Health Mini Program MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working MVP for a single-store wellness mini program: customer browsing and booking, therapist maintenance, scheduling, order fulfillment, and basic admin operations.

**Architecture:** Use a monorepo with a Spring Boot backend, a Vue 3 admin web app, and a native WeChat mini program frontend. The backend owns business rules for therapists, service items, schedules, appointment availability, and order state transitions; both frontends call the same REST API.

**Tech Stack:** Java 17, Spring Boot 3, Spring Data JPA, Flyway, MySQL for runtime, H2 for tests, Maven, Vue 3, Vite, TypeScript, Element Plus, native WeChat Mini Program, Vitest, JUnit 5.

---

## Scope

This plan implements only Phase 1 MVP from `docs/superpowers/specs/2026-06-12-health-mini-program-design.md`.

Included:

- User login stub based on a mini program session identity.
- Store information.
- Service category, service item list, and details.
- Therapist list, detail, maintenance, service bindings, and visibility.
- Therapist schedule setup with date-based shifts and blocked periods.
- Appointment availability calculation.
- Appointment creation, listing, detail, cancellation, arrival check-in, service start, and service completion.
- Admin data dashboard with core counts.
- Customer mini program pages for home, service items, therapists, booking, orders, and profile.

Excluded from MVP:

- Real WeChat Pay settlement.
- Subscription message delivery.
- Membership cards, coupons, points, group-buying, distribution, and multi-store operations.
- Independent therapist mobile workbench.

## File Structure

Create this structure:

```text
backend/
  pom.xml
  src/main/java/com/health/HealthApplication.java
  src/main/java/com/health/common/ApiResponse.java
  src/main/java/com/health/common/BadRequestException.java
  src/main/java/com/health/common/GlobalExceptionHandler.java
  src/main/java/com/health/store/
  src/main/java/com/health/serviceitem/
  src/main/java/com/health/therapist/
  src/main/java/com/health/schedule/
  src/main/java/com/health/appointment/
  src/main/java/com/health/user/
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init_schema.sql
  src/test/java/com/health/appointment/AppointmentAvailabilityServiceTest.java
  src/test/java/com/health/appointment/AppointmentServiceTest.java
  src/test/java/com/health/therapist/TherapistServiceTest.java
admin-web/
  package.json
  index.html
  vite.config.ts
  src/main.ts
  src/App.vue
  src/router.ts
  src/api/http.ts
  src/api/admin.ts
  src/pages/DashboardPage.vue
  src/pages/ServiceItemsPage.vue
  src/pages/TherapistsPage.vue
  src/pages/SchedulesPage.vue
  src/pages/AppointmentsPage.vue
  src/styles.css
miniprogram/
  app.json
  app.js
  app.wxss
  sitemap.json
  utils/api.js
  pages/home/
  pages/service-list/
  pages/service-detail/
  pages/therapist-list/
  pages/therapist-detail/
  pages/booking/
  pages/orders/
  pages/order-detail/
  pages/profile/
docs/api/mvp-api.md
README.md
```

Responsibilities:

- `backend`: source of truth for business data and all appointment rules.
- `admin-web`: internal operations UI for maintaining store, projects, therapists, schedules, and appointment state.
- `miniprogram`: customer-facing WeChat mini program UI.
- `docs/api/mvp-api.md`: concise contract for frontends and backend.

---

### Task 1: Backend Scaffold And Shared API Contract

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/health/HealthApplication.java`
- Create: `backend/src/main/java/com/health/common/ApiResponse.java`
- Create: `backend/src/main/java/com/health/common/BadRequestException.java`
- Create: `backend/src/main/java/com/health/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `README.md`

- [ ] **Step 1: Create Maven project configuration**

Create `backend/pom.xml` with Java 17, Spring Web, Validation, JPA, Flyway, MySQL, H2, and test dependencies.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.health</groupId>
  <artifactId>health-backend</artifactId>
  <version>0.1.0</version>
  <packaging>jar</packaging>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.6</version>
    <relativePath/>
  </parent>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-mysql</artifactId>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Create application entrypoint**

Create `backend/src/main/java/com/health/HealthApplication.java`.

```java
package com.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HealthApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthApplication.class, args);
    }
}
```

- [ ] **Step 3: Create consistent API response and exception handling**

Create `backend/src/main/java/com/health/common/ApiResponse.java`.

```java
package com.health.common;

public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

Create `backend/src/main/java/com/health/common/BadRequestException.java`.

```java
package com.health.common;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

Create `backend/src/main/java/com/health/common/GlobalExceptionHandler.java`.

```java
package com.health.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> badRequest(BadRequestException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("请求参数不合法");
        return ApiResponse.error(message);
    }
}
```

- [ ] **Step 4: Configure local profiles**

Create `backend/src/main/resources/application.yml`.

```yaml
spring:
  application:
    name: health-backend
  datasource:
    url: jdbc:mysql://localhost:3306/health?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: health
    password: health
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

server:
  port: 8080
```

- [ ] **Step 5: Add repository README**

Create `README.md`.

````markdown
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

Admin web:

```bash
cd admin-web
npm install
npm run dev
```

Mini program:

Open `miniprogram` in WeChat Developer Tools.
````
```

- [ ] **Step 6: Verify backend scaffold**

Run:

```bash
cd backend
mvn test
```

Expected: build succeeds with zero tests executed or the generated context test passing.

- [ ] **Step 7: Commit**

```bash
git add README.md backend
git commit -m "feat: scaffold backend service"
```

---

### Task 2: Database Schema And Seed Data

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/test/java/com/health/SchemaSmokeTest.java`

- [ ] **Step 1: Write a schema smoke test**

Create `backend/src/test/java/com/health/SchemaSmokeTest.java`.

```java
package com.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:health;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchemaSmokeTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesCoreTablesAndSeedStore() {
        Integer storeCount = jdbcTemplate.queryForObject("select count(*) from stores", Integer.class);
        Integer categoryCount = jdbcTemplate.queryForObject("select count(*) from service_categories", Integer.class);
        assertThat(storeCount).isEqualTo(1);
        assertThat(categoryCount).isGreaterThanOrEqualTo(3);
    }
}
```

- [ ] **Step 2: Run the smoke test to verify it fails**

Run:

```bash
cd backend
mvn -Dtest=SchemaSmokeTest test
```

Expected: FAIL because `V1__init_schema.sql` does not exist.

- [ ] **Step 3: Create schema migration**

Create `backend/src/main/resources/db/migration/V1__init_schema.sql`.

```sql
create table stores (
  id bigint primary key auto_increment,
  name varchar(80) not null,
  address varchar(255) not null,
  latitude decimal(10, 6),
  longitude decimal(10, 6),
  phone varchar(30) not null,
  business_start time not null,
  business_end time not null,
  announcement varchar(500),
  status varchar(20) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table users (
  id bigint primary key auto_increment,
  open_id varchar(80) not null unique,
  nickname varchar(80),
  avatar_url varchar(500),
  phone varchar(30),
  status varchar(20) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

create table service_categories (
  id bigint primary key auto_increment,
  name varchar(50) not null,
  sort_order int not null default 0,
  enabled boolean not null default true
);

create table service_items (
  id bigint primary key auto_increment,
  category_id bigint not null,
  name varchar(80) not null,
  image_url varchar(500),
  duration_minutes int not null,
  original_price decimal(10, 2) not null,
  sale_price decimal(10, 2) not null,
  suitable_people varchar(500),
  notice varchar(500),
  hot boolean not null default false,
  recommended boolean not null default false,
  status varchar(20) not null,
  sort_order int not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_service_items_category foreign key (category_id) references service_categories(id)
);

create table therapists (
  id bigint primary key auto_increment,
  store_id bigint not null,
  name varchar(50) not null,
  avatar_url varchar(500),
  gender varchar(20) not null,
  phone varchar(30) not null,
  employee_no varchar(50) not null,
  years_of_experience int not null default 0,
  level varchar(30) not null,
  status varchar(20) not null,
  introduction varchar(1000),
  specialties varchar(500),
  service_tags varchar(500),
  certificate_urls varchar(1000),
  bookable boolean not null default true,
  visible boolean not null default true,
  sort_order int not null default 0,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint fk_therapists_store foreign key (store_id) references stores(id),
  constraint uq_therapists_employee_no unique (store_id, employee_no)
);

create table therapist_service_items (
  therapist_id bigint not null,
  service_item_id bigint not null,
  primary key (therapist_id, service_item_id),
  constraint fk_tsi_therapist foreign key (therapist_id) references therapists(id),
  constraint fk_tsi_service_item foreign key (service_item_id) references service_items(id)
);

create table therapist_schedules (
  id bigint primary key auto_increment,
  therapist_id bigint not null,
  store_id bigint not null,
  schedule_date date not null,
  start_time time not null,
  end_time time not null,
  type varchar(20) not null,
  note varchar(255),
  created_at timestamp not null default current_timestamp,
  constraint fk_schedules_therapist foreign key (therapist_id) references therapists(id),
  constraint fk_schedules_store foreign key (store_id) references stores(id)
);

create table appointments (
  id bigint primary key auto_increment,
  user_id bigint not null,
  store_id bigint not null,
  service_item_id bigint not null,
  therapist_id bigint,
  appointment_date date not null,
  start_time time not null,
  end_time time not null,
  item_amount decimal(10, 2) not null,
  discount_amount decimal(10, 2) not null default 0,
  paid_amount decimal(10, 2) not null default 0,
  payment_status varchar(20) not null,
  status varchar(30) not null,
  contact_name varchar(50) not null,
  contact_phone varchar(30) not null,
  user_note varchar(500),
  admin_note varchar(500),
  created_at timestamp not null default current_timestamp,
  paid_at timestamp,
  arrived_at timestamp,
  service_started_at timestamp,
  completed_at timestamp,
  cancelled_at timestamp,
  constraint fk_appointments_user foreign key (user_id) references users(id),
  constraint fk_appointments_store foreign key (store_id) references stores(id),
  constraint fk_appointments_item foreign key (service_item_id) references service_items(id),
  constraint fk_appointments_therapist foreign key (therapist_id) references therapists(id)
);

create index idx_appointments_therapist_time
  on appointments (therapist_id, appointment_date, start_time, end_time, status);

insert into stores (name, address, latitude, longitude, phone, business_start, business_end, announcement, status)
values ('静养堂养生馆', '示例市示例区康养路 88 号', 31.230416, 121.473701, '021-88888888', '10:00:00', '23:00:00', '欢迎在线预约，到店请提前 10 分钟。', 'OPEN');

insert into service_categories (name, sort_order, enabled) values
('推拿理疗', 10, true),
('足疗养生', 20, true),
('艾灸调理', 30, true);

insert into service_items (category_id, name, image_url, duration_minutes, original_price, sale_price, suitable_people, notice, hot, recommended, status, sort_order)
values
(1, '肩颈舒缓推拿', '', 60, 198.00, 168.00, '长期伏案、肩颈酸胀人群', '饭后一小时内不建议体验。', true, true, 'ACTIVE', 10),
(2, '经典足疗', '', 60, 168.00, 138.00, '足部疲劳、久站人群', '皮肤破损处不建议体验。', true, false, 'ACTIVE', 20),
(3, '温阳艾灸调理', '', 45, 158.00, 128.00, '手脚冰凉、寒湿体质人群', '孕期用户请先咨询门店。', false, true, 'ACTIVE', 30);

insert into therapists (store_id, name, avatar_url, gender, phone, employee_no, years_of_experience, level, status, introduction, specialties, service_tags, certificate_urls, bookable, visible, sort_order)
values
(1, '李静', '', 'FEMALE', '13800000001', 'T001', 6, 'GOLD', 'ACTIVE', '擅长肩颈和腰背调理。', '肩颈,腰背,推拿', '手法稳,力度适中,复购高', '', true, true, 10),
(1, '王明', '', 'MALE', '13800000002', 'T002', 8, 'SENIOR', 'ACTIVE', '擅长足疗和经络放松。', '足疗,经络', '力度偏重,经验丰富', '', true, true, 20);

insert into therapist_service_items (therapist_id, service_item_id) values
(1, 1),
(1, 3),
(2, 1),
(2, 2);
```

- [ ] **Step 4: Run the schema test**

Run:

```bash
cd backend
mvn -Dtest=SchemaSmokeTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V1__init_schema.sql backend/src/test/java/com/health/SchemaSmokeTest.java
git commit -m "feat: add mvp database schema"
```

---

### Task 3: Service Item And Store Read APIs

**Files:**
- Create: `backend/src/main/java/com/health/store/Store.java`
- Create: `backend/src/main/java/com/health/store/StoreRepository.java`
- Create: `backend/src/main/java/com/health/store/StoreController.java`
- Create: `backend/src/main/java/com/health/serviceitem/ServiceCategory.java`
- Create: `backend/src/main/java/com/health/serviceitem/ServiceItem.java`
- Create: `backend/src/main/java/com/health/serviceitem/ServiceItemRepository.java`
- Create: `backend/src/main/java/com/health/serviceitem/ServiceItemController.java`
- Create: `backend/src/test/java/com/health/serviceitem/ServiceItemControllerTest.java`

- [ ] **Step 1: Write API tests for home data**

Create `backend/src/test/java/com/health/serviceitem/ServiceItemControllerTest.java`.

```java
package com.health.serviceitem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:items;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class ServiceItemControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void listsActiveServiceItems() throws Exception {
        mockMvc.perform(get("/api/service-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    void returnsStoreInfo() throws Exception {
        mockMvc.perform(get("/api/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("静养堂养生馆"))
                .andExpect(jsonPath("$.data.businessStart").value("10:00:00"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd backend
mvn -Dtest=ServiceItemControllerTest test
```

Expected: FAIL because controllers and entities are missing.

- [ ] **Step 3: Implement JPA entities and repositories**

Implement `Store`, `ServiceCategory`, and `ServiceItem` as JPA entities with column names matching the migration. Use `LocalTime` for business and service times, `BigDecimal` for money, and `boolean` for flags.

Required repository methods:

```java
package com.health.serviceitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findByStatusOrderBySortOrderAsc(String status);
}
```

```java
package com.health.store;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
```

- [ ] **Step 4: Implement read controllers**

`StoreController` must expose:

```java
GET /api/store
```

Return the first configured store.

`ServiceItemController` must expose:

```java
GET /api/service-items
GET /api/service-items/{id}
```

Only return service items with `status = ACTIVE` on the public list endpoint.

- [ ] **Step 5: Run tests**

Run:

```bash
cd backend
mvn -Dtest=ServiceItemControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/health/store backend/src/main/java/com/health/serviceitem backend/src/test/java/com/health/serviceitem
git commit -m "feat: expose store and service item APIs"
```

---

### Task 4: Therapist Maintenance APIs

**Files:**
- Create: `backend/src/main/java/com/health/therapist/Therapist.java`
- Create: `backend/src/main/java/com/health/therapist/TherapistRepository.java`
- Create: `backend/src/main/java/com/health/therapist/TherapistService.java`
- Create: `backend/src/main/java/com/health/therapist/TherapistController.java`
- Create: `backend/src/main/java/com/health/therapist/TherapistRequests.java`
- Create: `backend/src/test/java/com/health/therapist/TherapistServiceTest.java`

- [ ] **Step 1: Write therapist service tests**

Create `backend/src/test/java/com/health/therapist/TherapistServiceTest.java`.

```java
package com.health.therapist;

import com.health.common.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:therapists;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
class TherapistServiceTest {
    @Autowired
    TherapistService therapistService;

    @Test
    void publicListOnlyShowsActiveVisibleTherapists() {
        List<Therapist> therapists = therapistService.listPublicTherapists(1L);
        assertThat(therapists).extracting(Therapist::getName).contains("李静", "王明");
    }

    @Test
    void cannotCreateTherapistWithDuplicateEmployeeNumberInSameStore() {
        TherapistCreateRequest request = new TherapistCreateRequest(
                1L, "重复工号", "", "FEMALE", "13800000003", "T001", 3,
                "NORMAL", "ACTIVE", "测试", "肩颈", "手法稳", "", true, true, 30
        );

        assertThatThrownBy(() -> therapistService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("同一门店下技师工号不能重复");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd backend
mvn -Dtest=TherapistServiceTest test
```

Expected: FAIL because therapist classes are missing.

- [ ] **Step 3: Implement therapist domain**

Create `Therapist` entity mapped to `therapists`.

Create request records in `TherapistRequests.java`:

```java
package com.health.therapist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TherapistCreateRequest(
        @NotNull Long storeId,
        @NotBlank String name,
        String avatarUrl,
        @NotBlank String gender,
        @NotBlank String phone,
        @NotBlank String employeeNo,
        @Min(0) int yearsOfExperience,
        @NotBlank String level,
        @NotBlank String status,
        String introduction,
        String specialties,
        String serviceTags,
        String certificateUrls,
        boolean bookable,
        boolean visible,
        int sortOrder
) {}

public record TherapistUpdateRequest(
        @NotBlank String name,
        String avatarUrl,
        @NotBlank String gender,
        @NotBlank String phone,
        @Min(0) int yearsOfExperience,
        @NotBlank String level,
        @NotBlank String status,
        String introduction,
        String specialties,
        String serviceTags,
        String certificateUrls,
        boolean bookable,
        boolean visible,
        int sortOrder
) {}
```

Create repository methods:

```java
List<Therapist> findByStoreIdAndStatusAndVisibleOrderBySortOrderAsc(Long storeId, String status, boolean visible);
boolean existsByStoreIdAndEmployeeNo(Long storeId, String employeeNo);
```

- [ ] **Step 4: Implement service and controller**

`TherapistService` must support:

- `listPublicTherapists(Long storeId)`
- `listAdminTherapists(Long storeId)`
- `create(TherapistCreateRequest request)`
- `update(Long id, TherapistUpdateRequest request)`
- `changeStatus(Long id, String status)`

`TherapistController` must expose:

```text
GET /api/therapists
GET /api/therapists/{id}
GET /api/admin/therapists
POST /api/admin/therapists
PUT /api/admin/therapists/{id}
PATCH /api/admin/therapists/{id}/status
```

- [ ] **Step 5: Run tests**

Run:

```bash
cd backend
mvn -Dtest=TherapistServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/health/therapist backend/src/test/java/com/health/therapist
git commit -m "feat: add therapist maintenance APIs"
```

---

### Task 5: Schedule Management And Availability Calculation

**Files:**
- Create: `backend/src/main/java/com/health/schedule/TherapistSchedule.java`
- Create: `backend/src/main/java/com/health/schedule/TherapistScheduleRepository.java`
- Create: `backend/src/main/java/com/health/schedule/ScheduleController.java`
- Create: `backend/src/main/java/com/health/appointment/AppointmentAvailabilityService.java`
- Create: `backend/src/main/java/com/health/appointment/AvailableSlot.java`
- Create: `backend/src/test/java/com/health/appointment/AppointmentAvailabilityServiceTest.java`

- [ ] **Step 1: Write availability tests**

Create `backend/src/test/java/com/health/appointment/AppointmentAvailabilityServiceTest.java`.

```java
package com.health.appointment;

import com.health.schedule.TherapistSchedule;
import com.health.schedule.TherapistScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:availability;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
class AppointmentAvailabilityServiceTest {
    @Autowired
    AppointmentAvailabilityService availabilityService;

    @Autowired
    TherapistScheduleRepository scheduleRepository;

    @Test
    void returnsThirtyMinuteSlotsThatCanFitTheServiceDuration() {
        LocalDate date = LocalDate.now().plusDays(1);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        List<AvailableSlot> slots = availabilityService.availableSlots(1L, 1L, date);

        assertThat(slots).extracting(AvailableSlot::startTime)
                .contains(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0));
        assertThat(slots).extracting(AvailableSlot::startTime)
                .doesNotContain(LocalTime.of(11, 30));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd backend
mvn -Dtest=AppointmentAvailabilityServiceTest test
```

Expected: FAIL because schedule and availability services are missing.

- [ ] **Step 3: Implement schedule entity and repository**

Create `TherapistSchedule` mapped to `therapist_schedules` with fields from the migration.

Repository methods:

```java
List<TherapistSchedule> findByTherapistIdAndScheduleDateOrderByStartTimeAsc(Long therapistId, LocalDate scheduleDate);
List<TherapistSchedule> findByStoreIdAndScheduleDateOrderByStartTimeAsc(Long storeId, LocalDate scheduleDate);
```

- [ ] **Step 4: Implement availability service**

Create `AvailableSlot`.

```java
package com.health.appointment;

import java.time.LocalTime;

public record AvailableSlot(LocalTime startTime, LocalTime endTime) {
}
```

Rules:

- Use 30-minute increments.
- Read the selected service item duration.
- Read therapist schedules for the date.
- `WORK` periods add availability.
- `LEAVE`, `BLOCKED`, and `REST` periods remove availability.
- Existing appointments with statuses `PAID`, `ARRIVED`, `IN_SERVICE`, and `COMPLETED` block time.
- A slot is available only if the entire `[startTime, endTime)` fits inside a work period and does not overlap blocked periods or blocking appointments.

- [ ] **Step 5: Add admin schedule controller**

Expose:

```text
GET /api/admin/schedules?date=2026-06-13
POST /api/admin/schedules
DELETE /api/admin/schedules/{id}
GET /api/appointments/available-slots?therapistId=1&serviceItemId=1&date=2026-06-13
```

Request body for creating schedules:

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

- [ ] **Step 6: Run availability test**

Run:

```bash
cd backend
mvn -Dtest=AppointmentAvailabilityServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/health/schedule backend/src/main/java/com/health/appointment backend/src/test/java/com/health/appointment
git commit -m "feat: calculate appointment availability"
```

---

### Task 6: Appointment Creation And State Transitions

**Files:**
- Create: `backend/src/main/java/com/health/user/User.java`
- Create: `backend/src/main/java/com/health/user/UserRepository.java`
- Create: `backend/src/main/java/com/health/appointment/Appointment.java`
- Create: `backend/src/main/java/com/health/appointment/AppointmentRepository.java`
- Create: `backend/src/main/java/com/health/appointment/AppointmentService.java`
- Create: `backend/src/main/java/com/health/appointment/AppointmentController.java`
- Create: `backend/src/main/java/com/health/appointment/AppointmentRequests.java`
- Create: `backend/src/test/java/com/health/appointment/AppointmentServiceTest.java`

- [ ] **Step 1: Write appointment service tests**

Create `backend/src/test/java/com/health/appointment/AppointmentServiceTest.java`.

```java
package com.health.appointment;

import com.health.common.BadRequestException;
import com.health.schedule.TherapistSchedule;
import com.health.schedule.TherapistScheduleRepository;
import com.health.user.User;
import com.health.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none"
})
class AppointmentServiceTest {
    @Autowired
    AppointmentService appointmentService;

    @Autowired
    TherapistScheduleRepository scheduleRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void createsAppointmentWhenSlotIsAvailable() {
        User user = userRepository.save(new User(null, "openid-1", "测试用户", "", "13900000000", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        Appointment appointment = appointmentService.create(new AppointmentCreateRequest(
                user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", "肩颈酸"
        ));

        assertThat(appointment.getStatus()).isEqualTo("BOOKED");
        assertThat(appointment.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(appointment.getEndTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void rejectsOverlappingAppointment() {
        User user = userRepository.save(new User(null, "openid-2", "测试用户2", "", "13900000001", "ACTIVE"));
        LocalDate date = LocalDate.now().plusDays(1);
        scheduleRepository.save(new TherapistSchedule(null, 1L, 1L, date, LocalTime.of(10, 0), LocalTime.of(12, 0), "WORK", "morning"));

        AppointmentCreateRequest first = new AppointmentCreateRequest(user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 0), "张三", "13900000000", "");
        AppointmentCreateRequest second = new AppointmentCreateRequest(user.getId(), 1L, 1L, 1L, date, LocalTime.of(10, 30), "张三", "13900000000", "");

        appointmentService.create(first);

        assertThatThrownBy(() -> appointmentService.create(second))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("该时间段已被预约");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd backend
mvn -Dtest=AppointmentServiceTest test
```

Expected: FAIL because appointment service classes are missing.

- [ ] **Step 3: Implement appointment requests**

Create `AppointmentRequests.java`.

```java
package com.health.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentCreateRequest(
        @NotNull Long userId,
        @NotNull Long storeId,
        @NotNull Long serviceItemId,
        Long therapistId,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime startTime,
        @NotBlank String contactName,
        @NotBlank String contactPhone,
        String userNote
) {}

public record AppointmentStatusRequest(
        @NotBlank String status,
        String adminNote
) {}
```

- [ ] **Step 4: Implement user and appointment entities**

`User` maps to `users`. `Appointment` maps to `appointments`.

Use these appointment status constants in `AppointmentService`:

```java
private static final String STATUS_BOOKED = "BOOKED";
private static final String STATUS_ARRIVED = "ARRIVED";
private static final String STATUS_IN_SERVICE = "IN_SERVICE";
private static final String STATUS_COMPLETED = "COMPLETED";
private static final String STATUS_CANCELLED = "CANCELLED";
private static final String PAYMENT_UNPAID = "UNPAID";
```

- [ ] **Step 5: Implement creation rule**

`AppointmentService.create` must:

- Load user, store, service item, and therapist when `therapistId` is provided.
- Calculate `endTime` as `startTime + serviceItem.durationMinutes`.
- Call `AppointmentAvailabilityService.availableSlots` and require the requested start time to be present.
- Reject overlap with message `该时间段已被预约`.
- Persist `BOOKED` and `UNPAID`.
- Copy item sale price into `itemAmount`.
- Set `discountAmount` and `paidAmount` to zero.

- [ ] **Step 6: Implement state transition API**

Expose:

```text
POST /api/appointments
GET /api/appointments?userId=1
GET /api/appointments/{id}
PATCH /api/appointments/{id}/cancel
GET /api/admin/appointments?date=2026-06-13
PATCH /api/admin/appointments/{id}/arrive
PATCH /api/admin/appointments/{id}/start
PATCH /api/admin/appointments/{id}/complete
PATCH /api/admin/appointments/{id}/cancel
```

Allowed transitions:

```text
BOOKED -> ARRIVED
BOOKED -> CANCELLED
ARRIVED -> IN_SERVICE
ARRIVED -> CANCELLED
IN_SERVICE -> COMPLETED
```

Reject all other transitions with `当前订单状态不允许此操作`.

- [ ] **Step 7: Run tests**

Run:

```bash
cd backend
mvn -Dtest=AppointmentServiceTest test
```

Expected: PASS.

- [ ] **Step 8: Run backend suite**

Run:

```bash
cd backend
mvn test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/health/user backend/src/main/java/com/health/appointment backend/src/test/java/com/health/appointment
git commit -m "feat: add appointment lifecycle"
```

---

### Task 7: Admin Web Scaffold

**Files:**
- Create: `admin-web/package.json`
- Create: `admin-web/index.html`
- Create: `admin-web/vite.config.ts`
- Create: `admin-web/src/main.ts`
- Create: `admin-web/src/App.vue`
- Create: `admin-web/src/router.ts`
- Create: `admin-web/src/api/http.ts`
- Create: `admin-web/src/api/admin.ts`
- Create: `admin-web/src/styles.css`

- [ ] **Step 1: Create package configuration**

Create `admin-web/package.json`.

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vue-tsc --noEmit && vite build",
    "test": "vitest run"
  },
  "dependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "axios": "^1.7.9",
    "element-plus": "^2.9.1",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "typescript": "^5.7.2",
    "vite": "^6.0.3",
    "vitest": "^2.1.8",
    "vue-tsc": "^2.1.10"
  }
}
```

- [ ] **Step 2: Create Vite entry files**

Create `index.html`, `vite.config.ts`, and `src/main.ts`.

`src/main.ts` must mount Vue, Element Plus, router, and `src/styles.css`.

- [ ] **Step 3: Create layout and router**

`App.vue` must render a left navigation with:

- 数据看板
- 项目管理
- 技师管理
- 排班管理
- 预约订单

`router.ts` must map:

```text
/ -> DashboardPage
/service-items -> ServiceItemsPage
/therapists -> TherapistsPage
/schedules -> SchedulesPage
/appointments -> AppointmentsPage
```

- [ ] **Step 4: Create API wrapper**

Create `src/api/http.ts`.

```ts
import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && body.success === false) {
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body.data
})
```

- [ ] **Step 5: Run admin build**

Run:

```bash
cd admin-web
npm install
npm run build
```

Expected: PASS and Vite writes `dist`.

- [ ] **Step 6: Commit**

```bash
git add admin-web
git commit -m "feat: scaffold admin web"
```

---

### Task 8: Admin Pages For MVP Operations

**Files:**
- Create: `admin-web/src/pages/DashboardPage.vue`
- Create: `admin-web/src/pages/ServiceItemsPage.vue`
- Create: `admin-web/src/pages/TherapistsPage.vue`
- Create: `admin-web/src/pages/SchedulesPage.vue`
- Create: `admin-web/src/pages/AppointmentsPage.vue`
- Modify: `admin-web/src/api/admin.ts`

- [ ] **Step 1: Implement admin API methods**

Create `admin-web/src/api/admin.ts`.

```ts
import { http } from './http'

export type ServiceItem = {
  id: number
  name: string
  durationMinutes: number
  salePrice: number
  status: string
}

export type Therapist = {
  id: number
  name: string
  gender: string
  phone: string
  employeeNo: string
  level: string
  status: string
  visible: boolean
  bookable: boolean
}

export type Appointment = {
  id: number
  serviceItemId: number
  therapistId: number | null
  appointmentDate: string
  startTime: string
  endTime: string
  contactName: string
  contactPhone: string
  status: string
}

export const adminApi = {
  serviceItems: () => http.get<ServiceItem[]>('/api/service-items'),
  therapists: () => http.get<Therapist[]>('/api/admin/therapists', { params: { storeId: 1 } }),
  appointments: (date?: string) => http.get<Appointment[]>('/api/admin/appointments', { params: { date } }),
  arrive: (id: number) => http.patch(`/api/admin/appointments/${id}/arrive`),
  start: (id: number) => http.patch(`/api/admin/appointments/${id}/start`),
  complete: (id: number) => http.patch(`/api/admin/appointments/${id}/complete`),
  cancel: (id: number) => http.patch(`/api/admin/appointments/${id}/cancel`),
}
```

- [ ] **Step 2: Implement dashboard**

`DashboardPage.vue` must show:

- 今日预约数
- 今日已到店数
- 今日服务中数
- 今日已完成数
- 技师数量
- 上架项目数量

Compute these counts from `/api/admin/appointments` and local list APIs.

- [ ] **Step 3: Implement service item table**

`ServiceItemsPage.vue` must render an Element Plus table with item name, duration, sale price, and status.

- [ ] **Step 4: Implement therapist table**

`TherapistsPage.vue` must render name, gender, phone, employee number, level, status, bookable, and visible. Include create and edit dialogs wired to backend endpoints.

- [ ] **Step 5: Implement schedules page**

`SchedulesPage.vue` must allow choosing date and therapist, creating a `WORK`, `REST`, `LEAVE`, or `BLOCKED` time range, and listing that date's schedules.

- [ ] **Step 6: Implement appointments page**

`AppointmentsPage.vue` must list selected date appointments and expose buttons for arrival, start, complete, and cancel. Disable buttons when the current status cannot transition to that action.

- [ ] **Step 7: Run admin build**

Run:

```bash
cd admin-web
npm run build
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add admin-web/src
git commit -m "feat: add admin mvp pages"
```

---

### Task 9: Mini Program Scaffold And API Wrapper

**Files:**
- Create: `miniprogram/app.json`
- Create: `miniprogram/app.js`
- Create: `miniprogram/app.wxss`
- Create: `miniprogram/sitemap.json`
- Create: `miniprogram/utils/api.js`
- Create: page directories and empty page files listed in file structure

- [ ] **Step 1: Create app configuration**

Create `miniprogram/app.json`.

```json
{
  "pages": [
    "pages/home/index",
    "pages/service-list/index",
    "pages/service-detail/index",
    "pages/therapist-list/index",
    "pages/therapist-detail/index",
    "pages/booking/index",
    "pages/orders/index",
    "pages/order-detail/index",
    "pages/profile/index"
  ],
  "window": {
    "navigationBarTitleText": "静养堂养生馆",
    "navigationBarBackgroundColor": "#2f6b4f",
    "navigationBarTextStyle": "white",
    "backgroundColor": "#f6f7f5"
  },
  "tabBar": {
    "color": "#66706a",
    "selectedColor": "#2f6b4f",
    "list": [
      {
        "pagePath": "pages/home/index",
        "text": "首页"
      },
      {
        "pagePath": "pages/orders/index",
        "text": "订单"
      },
      {
        "pagePath": "pages/profile/index",
        "text": "我的"
      }
    ]
  }
}
```

- [ ] **Step 2: Create global state**

Create `miniprogram/app.js`.

```js
App({
  globalData: {
    apiBaseUrl: 'http://localhost:8080',
    userId: 1
  }
})
```

- [ ] **Step 3: Create API wrapper**

Create `miniprogram/utils/api.js`.

```js
const app = getApp()

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBaseUrl}${path}`,
      method: options.method || 'GET',
      data: options.data || {},
      success: (res) => {
        const body = res.data
        if (body && body.success) {
          resolve(body.data)
          return
        }
        reject(new Error((body && body.message) || '请求失败'))
      },
      fail: reject
    })
  })
}

module.exports = {
  getStore: () => request('/api/store'),
  getServiceItems: () => request('/api/service-items'),
  getServiceItem: (id) => request(`/api/service-items/${id}`),
  getTherapists: () => request('/api/therapists?storeId=1'),
  getTherapist: (id) => request(`/api/therapists/${id}`),
  getSlots: (therapistId, serviceItemId, date) => request(`/api/appointments/available-slots?therapistId=${therapistId}&serviceItemId=${serviceItemId}&date=${date}`),
  createAppointment: (data) => request('/api/appointments', { method: 'POST', data }),
  getOrders: (userId) => request(`/api/appointments?userId=${userId}`),
  getOrder: (id) => request(`/api/appointments/${id}`),
  cancelOrder: (id) => request(`/api/appointments/${id}/cancel`, { method: 'PATCH' })
}
```

- [ ] **Step 4: Create shared styles**

Create `miniprogram/app.wxss` with button, section, list, and price styles using a calm green, white background, and readable spacing.

- [ ] **Step 5: Create page stubs**

For each page directory, create:

```text
index.js
index.wxml
index.wxss
index.json
```

Each `index.json` must set a Chinese page title matching its purpose.

- [ ] **Step 6: Open in WeChat Developer Tools**

Run no shell command. Open `miniprogram` in WeChat Developer Tools and verify the app compiles.

Expected: the home tab opens without JavaScript errors.

- [ ] **Step 7: Commit**

```bash
git add miniprogram
git commit -m "feat: scaffold customer mini program"
```

---

### Task 10: Mini Program Customer Pages

**Files:**
- Modify: `miniprogram/pages/home/index.*`
- Modify: `miniprogram/pages/service-list/index.*`
- Modify: `miniprogram/pages/service-detail/index.*`
- Modify: `miniprogram/pages/therapist-list/index.*`
- Modify: `miniprogram/pages/therapist-detail/index.*`
- Modify: `miniprogram/pages/booking/index.*`
- Modify: `miniprogram/pages/orders/index.*`
- Modify: `miniprogram/pages/order-detail/index.*`
- Modify: `miniprogram/pages/profile/index.*`

- [ ] **Step 1: Implement home page**

Home page must load store, hot service items, and public therapists. It must show:

- Store name, address, phone, and business hours.
- Quick buttons for “预约项目” and “选择技师”.
- Three service item cards.
- Two therapist cards.

- [ ] **Step 2: Implement service list and detail**

Service list must show all active services. Detail must show name, duration, price, suitable people, notice, and “立即预约”.

Navigation:

```js
wx.navigateTo({ url: `/pages/booking/index?serviceItemId=${service.id}` })
```

- [ ] **Step 3: Implement therapist list and detail**

Therapist list must show active visible therapists. Detail must show introduction, specialties, tags, level, and “预约 TA”.

Navigation:

```js
wx.navigateTo({ url: `/pages/booking/index?therapistId=${therapist.id}` })
```

- [ ] **Step 4: Implement booking page**

Booking page must:

- Accept `serviceItemId` and `therapistId` from query.
- Require service item selection before loading slots.
- Allow therapist selection or default to the first available public therapist.
- Default booking date to tomorrow.
- Load slots from `/api/appointments/available-slots`.
- Collect contact name, contact phone, and note.
- Submit `POST /api/appointments`.
- Navigate to order detail after success.

Use this request shape:

```js
{
  userId: getApp().globalData.userId,
  storeId: 1,
  serviceItemId: Number(this.data.serviceItemId),
  therapistId: Number(this.data.therapistId),
  appointmentDate: this.data.date,
  startTime: this.data.selectedSlot.startTime,
  contactName: this.data.contactName,
  contactPhone: this.data.contactPhone,
  userNote: this.data.userNote
}
```

- [ ] **Step 5: Implement orders and order detail**

Orders page must list current user's appointments. Detail page must show project, therapist, date, time, contact info, status, and cancel button when status is `BOOKED`.

- [ ] **Step 6: Implement profile**

Profile must show a static current user card for MVP, order entry, store phone, and store address.

- [ ] **Step 7: Manual mini program verification**

In WeChat Developer Tools:

- Home page renders store info.
- Service list opens detail.
- Therapist list opens detail.
- Booking page displays slots for seeded schedule data after admin creates a schedule.
- Creating an appointment redirects to order detail.
- Cancelling a `BOOKED` appointment changes its status.

- [ ] **Step 8: Commit**

```bash
git add miniprogram/pages miniprogram/utils miniprogram/app.*
git commit -m "feat: add customer booking flow"
```

---

### Task 11: MVP API Documentation

**Files:**
- Create: `docs/api/mvp-api.md`

- [ ] **Step 1: Document response envelope**

Create `docs/api/mvp-api.md` with:

````markdown
# MVP API

All responses use:

```json
{
  "success": true,
  "data": {},
  "message": ""
}
```
````

- [ ] **Step 2: Document public endpoints**

Include:

```text
GET /api/store
GET /api/service-items
GET /api/service-items/{id}
GET /api/therapists?storeId=1
GET /api/therapists/{id}
GET /api/appointments/available-slots?therapistId=1&serviceItemId=1&date=2026-06-13
POST /api/appointments
GET /api/appointments?userId=1
GET /api/appointments/{id}
PATCH /api/appointments/{id}/cancel
```

- [ ] **Step 3: Document admin endpoints**

Include:

```text
GET /api/admin/therapists?storeId=1
POST /api/admin/therapists
PUT /api/admin/therapists/{id}
PATCH /api/admin/therapists/{id}/status
GET /api/admin/schedules?date=2026-06-13
POST /api/admin/schedules
DELETE /api/admin/schedules/{id}
GET /api/admin/appointments?date=2026-06-13
PATCH /api/admin/appointments/{id}/arrive
PATCH /api/admin/appointments/{id}/start
PATCH /api/admin/appointments/{id}/complete
PATCH /api/admin/appointments/{id}/cancel
```

- [ ] **Step 4: Commit**

```bash
git add docs/api/mvp-api.md
git commit -m "docs: add mvp api contract"
```

---

### Task 12: End-To-End Smoke Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Run backend tests**

Run:

```bash
cd backend
mvn test
```

Expected: PASS.

- [ ] **Step 2: Run admin build**

Run:

```bash
cd admin-web
npm run build
```

Expected: PASS.

- [ ] **Step 3: Start backend**

Run:

```bash
cd backend
mvn spring-boot:run
```

Expected: backend listens on `http://localhost:8080`.

- [ ] **Step 4: Start admin web**

Run in a second terminal:

```bash
cd admin-web
npm run dev
```

Expected: Vite serves the admin UI and prints a local URL.

- [ ] **Step 5: Manual smoke path**

Verify:

- Admin can view seeded service items.
- Admin can view seeded therapists.
- Admin can create a work schedule for tomorrow from 10:00 to 18:00.
- Mini program can choose a service, choose a therapist, see available slots, and create an appointment.
- Admin appointment list shows the appointment.
- Admin can move the appointment through arrived, in service, and completed.
- Mini program order detail reflects the completed status.

- [ ] **Step 6: Update README with verified run path**

Add a "MVP Smoke Test" section containing the exact steps from Step 5 and the expected result for each one.

- [ ] **Step 7: Commit**

```bash
git add README.md
git commit -m "docs: add mvp smoke test steps"
```

---

## Plan Self-Review

Spec coverage:

- Product positioning as single-store MVP: covered by scope and all tasks using `storeId`.
- User mini program browsing and booking: covered by Tasks 9 and 10.
- Therapist information maintenance: covered by Task 4 and admin page work in Task 8.
- Online appointment module: covered by Tasks 5, 6, 8, and 10.
- Backend order fulfillment: covered by Task 6 and Task 8.
- Store information and service items: covered by Tasks 2 and 3.
- Notifications, real payments, membership, coupons, points, and multi-store: intentionally excluded from MVP and preserved as future scope.

Placeholder scan:

- The plan contains no unspecified placeholder steps.
- All high-risk rules have concrete tests or manual smoke checks.

Type consistency:

- Appointment statuses use `BOOKED`, `ARRIVED`, `IN_SERVICE`, `COMPLETED`, and `CANCELLED`.
- Payment status uses `UNPAID` for MVP.
- Schedule types use `WORK`, `REST`, `LEAVE`, and `BLOCKED`.
- Public and admin REST paths are consistent across backend, admin web, mini program, and API documentation.
