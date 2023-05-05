(ns sugbi.catalog.core
 (:require
  [clojure.set :as set]
  [sugbi.catalog.db :as db]
  [sugbi.catalog.open-library-books :as olb]))


(defn merge-on-key
  [k x y]
  (->> (concat x y)
       (group-by k)
       (map val)
       (mapv (fn [[x y]] (merge x y)))))


(def available-fields olb/relevant-fields)


(defn get-book
  [isbn fields]
  (when-let [db-book (db/get-book {:isbn isbn})]
    (let [open-library-book-info (olb/book-info isbn fields)
          availability (db/get-book-availability {:isbn isbn})]
      (-> db-book
          (merge open-library-book-info)
          (assoc :available (:count availability))
          (dissoc :book_id)))))


(defn get-books
  [fields]
  (let [db-books (db/get-books {})
        isbns (map :isbn db-books)
        open-library-book-infos (olb/multiple-book-info isbns fields)]
    (->> (merge-on-key :isbn db-books open-library-book-infos)
         (map (fn [book]
                (let [availability (db/get-book-availability {:isbn (:isbn book)})]
                  (assoc book :available (:count availability))))))))


(defn enriched-search-books-by-title
  [title fields]
  (let [db-book-infos (db/matching-books title)
        isbns (map :isbn db-book-infos)
        open-library-book-infos (olb/multiple-book-info isbns fields)]
    (->> (merge-on-key :isbn db-book-infos open-library-book-infos)
         (map (fn [book]
                (let [availability (db/get-book-availability {:isbn (:isbn book)})]
                  (assoc book :available (:count availability))))))))


(defn checkout-book
  [isbn book_item_id user_id]
  (let [book (db/get-book {:isbn isbn})
        book_id_from_item (db/get-book-id-from-item-id {:book_item_id book_item_id})
        item-availability (db/get-item-availability {:book_item_id book_item_id})]
    (when (and (:available item-availability)
               (= (:book_id book) (:book_id book_id_from_item)))
      (db/checkout-book! {:book_item_id book_item_id
                          :user_id user_id}))))