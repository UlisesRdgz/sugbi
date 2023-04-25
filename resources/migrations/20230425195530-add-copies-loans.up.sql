create table catalog.copies (
  copy_id bigint generated always as identity primary key,
  book_id bigint not null references catalog.book(book_id),
  status varchar(32) not null default 'available'
);
--;;
create table catalog.loans (
  loan_id bigint generated always as identity primary key,
  copy_id bigint not null references catalog.copies(copy_id),
  user_id bigint not null,
  loan_date date not null,
  due_date date not null
);