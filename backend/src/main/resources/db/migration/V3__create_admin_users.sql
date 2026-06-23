create table admin_users (
  id bigint primary key auto_increment,
  username varchar(50) not null unique,
  password_hash varchar(100) not null,
  display_name varchar(80) not null,
  status varchar(20) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp
);

insert into admin_users (username, password_hash, display_name, status)
values ('admin', '$2a$10$aF.v8gapWefGu9VuYDdzQemIQlmrhPm0hMKJmYk5.xXinfngX92W.', '系统管理员', 'ACTIVE');
