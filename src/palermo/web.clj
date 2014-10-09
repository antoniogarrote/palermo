(ns palermo.web
  (:require [compojure.core :refer [defroutes GET DELETE PUT]]
             [compojure.route :as route]
             [compojure.handler :as handler]
             [ring.adapter.jetty :as ring]
             [ring.middleware.resource :refer [wrap-resource]]
             [ring.util.response :as response]
             [palermo.web.views :as views])
   (:import [palermo PalermoServer]))

(def PALERMO (atom nil))


(defroutes routes
  (GET "/queues" []
       (views/all-queues @PALERMO))
  (GET "/workers" []
       (views/all-workers @PALERMO))
  (DELETE "/queues/:name/purge" [name]
          (.purgeQueue @PALERMO name)
          (response/redirect (str "/queue/" name)))
  (GET "/retry/:id" [id]
       (.retryJob @PALERMO id)
       (response/redirect "/failures"))
  (PUT "/failures/retry_all" []
       (.retryAllFailedJobs @PALERMO)
       (response/redirect "/failures"))
  (GET "/failures" []
       (views/failures @PALERMO))
  (GET "/queue/:name" [name]
       (views/queue @PALERMO name))
  (GET "/" [] (views/index @PALERMO))
  (route/resources "/"))
  
  
(defn start [server-port host port username password vhost exchange]
  (swap! PALERMO (fn [old_value] (PalermoServer. host port username password exchange vhost)))
  (ring/run-jetty 
   (handler/site routes)
   {:port server-port
    :join? false}))
  
(defn start-dev [args]
  (swap! PALERMO (fn [old_value] (PalermoServer.)))
  ((handler/site routes) args))



;(defn -main []
;  (ring/run-jetty #'routes {:port 8080 :join? false}))

;(def ^:dynamic *server* (start 8080 "localhost" (int 5672) "guest" "guest" "/" "palermo"))
;(.stop *server*)
