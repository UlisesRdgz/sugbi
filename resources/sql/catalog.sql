-- :name insert-book! :! :1
insert into catalog.book (title, isbn) values (:title, :isbn)
returning *;

-- :name delete-book! :! :n
delete from catalog.book where isbn = :isbn;

-- :name search :? :*
select isbn, true as "available"
from catalog.book
where lower(title) like :title;

-- :name get-book :? :1
select isbn, true as "available"
from catalog.book
where isbn = :isbn

-- :name get-books :? :*
select isbn, true as "available"
from catalog.book;

-- :name checkout-book :! :1
with lending as (
    insert into catalog.lendings (copy_id, user_id, lending_date, due_date)
    values (:book_item_id, :user_id, now(), now() + interval '14 days')
    returning *
)
update catalog.items
set available = false
where item_id = :book_item_id;

-- :name return-book :! :n
with return_book as (
    update catalog.lendings
    set due_date = now()
    where copy_id = :book_item_id
    and user_id = :user_id
    and due_date > now()
)
update catalog.items
set available = true
where item_id = :book_item_id

-- :name get-book-lendings :? :*
select * from catalog.lendings
where user_id = :user_id
and due_date >= now();

-- :name get-book-availability :? :1
select count(*) from catalog.items 
where book_id = (select book_id from catalog.book where isbn = :isbn)
and available = true

-- :name insert-item! :! :1
insert into catalog.items (book_id, available) values (:book_id, true)
returning *;