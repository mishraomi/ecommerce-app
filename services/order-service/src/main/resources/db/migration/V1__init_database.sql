create table if not exists orders (
    id bigint not null auto_increment,
    user_id varchar(255) not null,
    order_status varchar(20) not null,
    order_date datetime not null,
    total_amount decimal(10, 2) not null,
    primary key (id)
);

create table if not exists order_items (
    id bigint not null auto_increment,
    product_id bigint not null,
    quantity integer not null,
    price decimal(10, 2) not null,
    order_id bigint not null,
    primary key (id),
    foreign key (order_id) references orders(id) on delete cascade
); 