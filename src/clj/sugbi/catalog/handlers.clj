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
      (if (catalog.core/checkout-book isbn book-item-id user)
        (response/ok {:status 200 :message "Book checked out successfully"})
        (response/status 409 {:message "Book is already checked out"}))
      (response/forbidden {:message "You need an active session to perform this action"}))))


(defn return-book!
  [request]
  (let [isbn         (get-in request [:parameters :path :isbn])
        book-item-id (get-in request [:parameters :path :book-item-id])
        user         (get-in request [:session :user])]
    (if user
      (if (catalog.db/return-book! isbn book-item-id user)
        (response/ok {:status 200 :message "Book returned successfully"})
        (response/not-found {:message "Book not found"}))
      (response/forbidden {:message "You need an active session to perform this action"}))))
