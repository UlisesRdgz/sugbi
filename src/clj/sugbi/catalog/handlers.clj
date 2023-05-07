(ns sugbi.catalog.handlers
  (:require
   [ring.util.http-response :as response]
   [sugbi.catalog.db :as catalog.db]
   [sugbi.catalog.core :as catalog.core]
   [sugbi.auth.google.handlers :as google.handlers]))


(defn search-books
  [request]
  (if-let [criteria (get-in request [:parameters :query :q])]
    (response/ok
     (catalog.core/enriched-search-books-by-title
      criteria
      catalog.core/available-fields))
    (response/ok
     (catalog.core/get-books
      catalog.core/available-fields))))


(defn insert-book!
  [request]
  (let [{:keys [_isbn _title]
         :as book-info} (get-in request [:parameters :body])
        is-librarian?   (get-in request [:session :is-librarian?])]
    (if is-librarian?
      (response/ok
       (select-keys (catalog.db/insert-book! book-info) [:isbn :title]))
      (response/forbidden {:message "Operation restricted to librarians"}))))


(defn delete-book!
  [request]
  (let [isbn          (get-in request [:parameters :path :isbn])
        is-librarian? (get-in request [:session :is-librarian?])]
    (if is-librarian?
      (response/ok
       {:deleted (catalog.db/delete-book! {:isbn isbn})})
      (response/forbidden {:message "Operation restricted to librarians"}))))


(defn get-book
  [request]
  (let [isbn (get-in request [:parameters :path :isbn])]
    (if-let [book-info (catalog.core/get-book
                        isbn
                        catalog.core/available-fields)]
      (response/ok book-info)
      (response/not-found {:isbn isbn}))))


(defn checkout-book!
  [request]
  (let [isbn         (get-in request [:parameters :path :isbn])
        book-item-id (get-in request [:parameters :path :book-item-id])
        user         (get-in google.handlers/callback-data [:user-info :sub])]
    (if user
      (if (catalog.core/checkout-book isbn book-item-id)
        (if (empty? (catalog.db/get-loan {:user_id user :copy_id book-item-id}))
          (response/ok
           (select-keys (catalog.db/checkout-book! {:user_id user :book_item_id book-item-id})
                        [:lending_id :lending_date :due_date]))
          (response/conflict {:message "Book is already checked out"}))
        (response/not-found {:message "Book does not match the provided ISBN or book-item-id"}))
      (response/forbidden {:message "You need an active session to perform this action"}))))


(defn return-book!
  [request]
  (let [isbn         (get-in request [:parameters :path :isbn])
        book-item-id (get-in request [:parameters :path :book-item-id])
        user         (get-in google.handlers/callback-data [:user-info :sub])]
    (if user
      (if (catalog.core/checkout-book isbn book-item-id)
        (if (empty? (catalog.db/get-loan {:user_id user :copy_id book-item-id}))
          (response/forbidden {:message "The book is not currently checked out to the user"})
          (response/ok
           (select-keys (catalog.db/return-book! {:user_id user :book_item_id book-item-id})
                        [:lending_id :lending_date :due_date])))
        (response/not-found {:message "Book does not match the provided ISBN or book-item-id"}))
      (response/forbidden {:message "You need an active session to perform this action"}))))


(defn search-lendings
  [request]
  (let [user         (get-in google.handlers/callback-data [:user-info :sub])]
    (if user
      (response/ok
       (catalog.db/get-book-lendings {:user_id user}))
      (response/forbidden {:message "You need an active session to perform this action"}))))


(defn search-lendings-user-id
  [request]
  (let [user-id          (get-in request [:parameters :path :user-id])
        is-librarian?    (get-in request [:session :is-librarian?])]
    (if is-librarian?
      (if (empty? (catalog.db/get-book-lendings {:user_id user-id}))
        (response/not-found {:message "User does not exist"}) 
        (response/ok
         (catalog.db/get-book-lendings {:user_id user-id})))
      (response/forbidden {:message "Operation restricted to librarians"}))))

;; (not (nil? (catalog.db/get-loan {:user_id "115924144694284890141" :copy_id 1})))
;; (empty? (catalog.db/get-loan {:user_id "115924144694284890141" :copy_id 11}))
;; (catalog.db/get-loan {:user_id "115924144694284890141" :copy_id 1})
