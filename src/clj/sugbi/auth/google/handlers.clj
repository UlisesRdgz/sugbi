(ns sugbi.auth.google.handlers
  (:require
   [ring.util.http-response :as response]
   [sugbi.auth.google.core :as auth.google]
   [sugbi.config :refer [env]]
   [sugbi.user-management.core :as user-management.core]))


(defn authorize-handler
  [callback-uri]
  (fn [_]
    (response/found
     (auth.google/oauth-authorize-uri
      (:google/client env)
      callback-uri
      (:scopes-supported auth.google/openid-configuration)))))


(defn callback-handler
  [callback-uri]
  (fn [request]
    (def request request)
    (let [code          (get-in request [:params :code])
          access-token  (auth.google/oauth-access-token
                         (:google/client env)
                         (:google/secret env)
                         code
                         callback-uri
                         (:scopes-supported auth.google/openid-configuration))
          user-info     (auth.google/user-info access-token)
          is-librarian? (user-management.core/is-librarian? (:sub user-info))]
      (def callback-data
        {:code code
         :access-token access-token
         :user-info user-info
         :is-librarian? is-librarian?
         :session (:session request)})
      (assoc (response/found (str "http://" (get-in request [:headers "host"]) "/api/api-docs/index.html"))
             :session (merge (:session request) user-info {:is-librarian? is-librarian?})))))
