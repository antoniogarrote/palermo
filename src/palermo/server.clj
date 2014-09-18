(ns palermo.server
  (:require [palermo.rabbit :as prabbit]
            [palermo.job :as pjob])
  (:gen-class
    :init init
    :name palermo.Server3
    :constructors {[String Integer String String String] []
                   [String Integer String] []
                   [String] []
                   [] []}
    :methods [[show [] clojure.lang.PersistentArrayMap]
              [enqueue [String java.lang.Class Object] void]
              [setSerialization [String] void]
              [getSerialization [] String]]
    :state state))


(defn connect 
  "Establish a connection to RabbitMQ"
  [{:keys [host port username password exchange]}]
  (prabbit/connect host port username password))

(defn -init 
  ([]
     [[] (let [connection (connect {:host "localhost" :port 5672 :username "guest" :password "guest"})]
           (atom {:connection connection
                  :exchange "palermo"
                  :serialization "application/json"}))])
  ([exchange] 
     [[] (let [connection (connect {:host "localhost" :port 5672 :username "guest" :password "guest"})]
           (atom {:connection connection
                  :exchange exchange
                  :serialization "application/json"}))])
  ([host port username password exchange]
     [[] (let [connection (connect {:host host :port port :username username :password password})]
           (atom {:connection connection
                  :exchange exchange
                  :serialization "application/json"}))])
  ([host port exchange]
     [[] (let [connection (connect {:host host :port port :username "guest" :password "guest"})]
           (atom {:connection connection
                  :exchange exchange
                  :serialization "application/json"}))]))

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

(defn -show [this]
  (deref (.state this)))




;; (def pal (new palermo.server "a" (int 1) "b" "c" "d"))
