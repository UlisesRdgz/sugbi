create table catalog.items (
  item_id bigint generated always as identity primary key,
  book_id bigint not null references catalog.book(book_id),
  status boolean not null
);
--;;
create table catalog.lendings (
  lending_id bigint generated always as identity primary key,
  copy_id bigint not null references catalog.items(item_id),
  user_id bigint not null,
  lending_date date not null,
  due_date date not null
);