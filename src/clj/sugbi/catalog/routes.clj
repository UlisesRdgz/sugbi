(ns sugbi.catalog.routes
  (:require
   [clojure.spec.alpha :as s]
   [reitit.ring.middleware.multipart :as multipart]
   [spec-tools.data-spec :as ds]
   [sugbi.catalog.handlers :as catalog.handlers]
   [clojure.string :as string])
  (:import java.io.BufferedInputStream
           java.time.LocalDate))


(defn date?
  [o]
  (instance? java.time.LocalDate o))


(def basic-book-info-spec
  {:isbn  string?
   :title string?})


(def book-info-spec
  {:isbn                         string?
   :available                    int?
   (ds/opt :title)               string?
   (ds/opt :full-title)          string?
   (ds/opt :subtitle)            string?
   (ds/opt :publishers)          [string?]
   (ds/opt :publish-date)        string?
   (ds/opt :weight)              string?
   (ds/opt :physical-dimensions) string?
   (ds/opt :genre)               string?
   (ds/opt :subjects)            [string?]
   (ds/opt :number-of-pages)     int?})


(def loan-spec
  {:lending_id     int?
   :copy_id        int?
   :user_id        string?
   :lending_date   date?
   :due_date       date?})


(def routes 
  [""
   ["/catalog" {:swagger {:tags ["Catalog"]}}
    ["/books"
     ["" {:get  {:summary    "gets the catalog. Optionally, accepts a search criteria"
                 :parameters {:query {(ds/opt :q) string?}}
                 :responses  {200 {:body [book-info-spec]}}
                 :handler    catalog.handlers/search-books}
          :post {:summary    "add a book title to the catalog"
                 :parameters {:header {:cookie string?}
                              :body   basic-book-info-spec}
                 :responses  {200 {:body basic-book-info-spec}
                              405 {:body {:message string?}}}
                 :handler    catalog.handlers/insert-book!}}]
     ["/:isbn"
      ["" {:get    {:summary    "get a book info by its isbn"
                    :parameters {:path {:isbn string?}}
                    :responses  {200 {:body book-info-spec}
                                 404 {:body {:isbn string?}}}
                    :handler    catalog.handlers/get-book}
           :delete {:summary    "delete a book title of the catalog"
                    :parameters {:header {:cookie string?}
                                 :path   {:isbn string?}}
                    :responses  {200 {:body {:deleted int?}}
                                 405 {:body {:message string?}}}
                    :handler    catalog.handlers/delete-book!}}]
      ["/item"
       ["/:book-item-id"
        ["/checkout" {:post {:summary    "request a lending with the book item id."
                             :parameters {:path {:isbn string?
                                                 :book-item-id int?}}
                             :responses  {200 {:body {:message string?}}
                                          404 {:body {:message string?}}
                                          409 {:body {:message string?}}
                                          403 {:body {:message string?}}}
                             :handler    catalog.handlers/checkout-book!}}]
        ["/return" {:post {:summary    "returns a lending with the book item id."
                           :parameters {:path {:isbn string?
                                               :book-item-id int?}}
                           :responses  {200 {:body {:returned int?}}
                                        404 {:body {:message string?}}
                                        403 {:body {:message string?}}}
                           :handler    catalog.handlers/return-book!}}]]]]]]
   ["" {:swagger {:tags ["Lendings"]}}
    ["/user" 
     ["/lendings" {:get  {:summary    "gets the lendings of the actual user."
                          :responses  {200 {:body [loan-spec]}
                                       403 {:body {:message string?}}}
                          :handler    catalog.handlers/search-lendings}}]]
    ["/lendings" {:get  {:summary    "gets the lendings of the user by their user-id."
                         :parameters {:header {:cookie string?}
                                      :query {:user-id string?}}
                         :responses  {200 {:body [loan-spec]}
                                      404 {:body {:message string?}}
                                      403 {:body {:message string?}}}
                         :handler    catalog.handlers/search-lendings-user-id}}]]])
