create table items (sku varchar(32) primary key, total bigint, available bigint, held bigint, sold bigint);
create table reservations (id bigint auto_increment primary key, request_id varchar(64) unique, state varchar(16), created_at bigint, expires_at bigint, ttl_millis bigint);
create table reservation_lines (id bigint auto_increment primary key, reservation_id bigint, sku varchar(32), quantity bigint);
create table settings (name varchar(32) primary key, val bigint);
