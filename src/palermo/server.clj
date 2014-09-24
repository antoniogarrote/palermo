(ns palermo.server
  (:require [palermo.rabbit :as prabbit]
            [palermo.job    :as pjob]
            [palermo.worker :as pworker])
  (:gen-class
    :init init
    :name palermo.Server
    :constructors {[String Integer String String String String] []
                   [String Integer String] []
                   [String] []
                   [] []}
    :methods [[show [] clojure.lang.PersistentArrayMap]
              [enqueue [String java.lang.Class Object] void]
              [startWorker ["[Ljava.lang.String;"] String]
              [stopWorker [String] void]
              [workers [] "[Ljava.lang.String;"]
              [setSerialization [String] void]
              [getSerialization [] String]]
    :state state))

(defn connect 
  "Establish a connection to RabbitMQ"
  [{:keys [host port username password exchange vhost]}]
  (prabbit/connect host port username password vhost))

(defn -init 
  ([]
     [[] (let [connection (connect {:host "localhost" :port 5672 :username "guest" :password "guest" :vhost "/"})]
           (atom {:connection connection
                  :vhost "/"
                  :exchange "palermo"
                  :serialization "application/json"
                  :consumers {}}))])
  ([exchange] 
     [[] (let [connection (connect {:host "localhost" :port 5672 :username "guest" :password "guest" :vhost "/"})]
           (atom {:connection connection
                  :vhost "/"
                  :exchange exchange
                  :serialization "application/json"
                  :consumers {}}))])
  ([host port username password exchange vhost]
     [[] (let [connection (connect {:host host :port port :username username :password password :vhost vhost})]
           (atom {:connection connection
                  :vhost vhost
                  :exchange exchange
                  :serialization "application/json"
                  :consumers {}}))])
  ([host port exchange]
     [[] (let [connection (connect {:host host :port port :username "guest" :password "guest" :vhost "/"})]
           (atom {:connection connection
                  :vhost "/"
                  :exchange exchange
                  :serialization "application/json"
                  :consumers {}}))]))

(defn -setSerialization [this serialization]
  (swap! (.state this) assoc :serialization serialization))

(defn -getSerialization [this]
  (:serialization (deref (.state this))))

(defn -enqueue [this queue job-class arguments]
  (let [serialization-type (:serialization (deref (.state this)))
        exchange-name (:exchange (deref (.state this)))
        id (str (java.util.UUID/randomUUID))
        job-message (pjob/make-job-message serialization-type job-class arguments {:id id})
        ch (prabbit/channel (:connection (deref (.state this))))]
    (prabbit/publish-job-messages ch exchange-name queue job-message)))

(defn -startWorker [this queues]
  (let [worker-id (str (java.util.UUID/randomUUID))
        channel (prabbit/channel (:connection (deref (.state this))))
        exchange-name (:exchange (deref (.state this)))
        worker-tags (pworker/start-worker channel exchange-name queues)
        old-workers (:workers (deref (.state this)))]
    (swap! (.state this) assoc :workers (assoc old-workers worker-id {:channel channel
                                                                      :tags worker-tags}))
    worker-id))


(defn -workers [this]
  (let [workers (:workers (deref (.state this)))]
    (into-array String (keys workers))))

(defn -stopWorker [this worker-id]
  (let [workers (:workers (deref (.state this)))
        worker-info (get  workers worker-id)
        tags (:tags worker-info)
        channel (:channel worker-info)]
    (doseq [tag tags] (prabbit/cancel-subscriber channel tag))
    (try (prabbit/close-channel channel)
         (catch Exception e nil))
    (swap! (.state this) assoc :workers (dissoc workers worker-id))))
  
(defn -show [this]
  (deref (.state this)))
