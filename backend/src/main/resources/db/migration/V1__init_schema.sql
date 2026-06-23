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
