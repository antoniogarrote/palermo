(ns palermo.web
  (:require [compojure.core :refer [defroutes GET DELETE PUT]]
             [compojure.route :as route]
             [compojure.handler :as handler]
             [ring.adapter.jetty :as ring]
             [ring.middleware.resource :refer [wrap-resource]]
             [ring.util.response :as response]
             [palermo.web.views :as views])
   (:import [palermo PalermoServer]))


(defn make-routes [host port username password vhost exchange]
  (let [palermo (PalermoServer. host port username password exchange vhost)]
    (defroutes routes
      (GET "/queues" []
           (views/all-queues palermo))
      (GET "/workers" []
           (views/all-workers palermo))
      (DELETE "/queues/:name/purge" [name]
              (.purgeQueue palermo name)
              (response/redirect (str "/queue/" name)))
      (GET "/retry/:id" [id]
           (.retryJob palermo id)
           (response/redirect "/failures"))
      (PUT "/failures/retry_all" []
           (.retryAllFailedJobs palermo)
           (response/redirect "/failures"))
      (GET "/failures" []
           (views/failures palermo))
      (GET "/queue/:name" [name]
           (views/queue palermo name))
      (GET "/" [] (views/index palermo))
      (route/resources "/"))))
  
  
(defn start [server-port host port username password vhost exchange]
  (ring/run-jetty 
   (handler/site (make-routes host port username password vhost exchange))
   {:port server-port
    :join? false}))
  
(def start-dev 
  (handler/site (make-routes "localhost" (int 5672) "guest" "guest" "/" "palermo")))



;(defn -main []
;  (ring/run-jetty #'routes {:port 8080 :join? false}))

;(def ^:dynamic *server* (start 8080 "localhost" (int 5672) "guest" "guest" "/" "palermo"))
;(.stop *server*)
