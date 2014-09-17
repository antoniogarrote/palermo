(ns palermo.rabbit
  (:require [langohr.core :as rmq]
            [langohr.basic :as lbasic]
            [langohr.channel :as lchannel]
            [langohr.exchange :as lexchange]
            [langohr.queue :as lqueue]
            [langohr.consumers :as lconsumers]
            [palermo.serialisation :as pserialisation]
            [palermo.job :as pjob]))

(defn connect
  "Connects to the RabbitMQ broker"
  ([host port username password]
     (rmq/connect {:host host :port port :username username :password password}))
  ([]
     (rmq/connect)))

(defn channel
  "Opens a Langohr channel using the provided connection"
  [connection]
  (lchannel/open connection))


(defn consume
  "Starts a consumer attached to a queue and bound to a a certain topic"
  [ch exchange-name queue-name handler]
  (let [topic-name queue-name
        data-handler    (fn [ch metadata ^bytes payload]
                          (try  
                            (let [media-type (:content-type metadata)
                                  serialiser (pserialisation/make-serialiser media-type)
                                  content (pserialisation/read serialiser payload)
                                  job-message (pjob/job-message media-type content metadata)]
                              (handler job-message))
                            (catch Exception e
                              (println (.getMessage e)))))]
    (lexchange/declare ch exchange-name "direct")
    (lqueue/declare ch queue-name {:exclusive false :auto-delete false})
    (lqueue/bind    ch queue-name exchange-name {:routing-key topic-name})
    (lconsumers/subscribe ch queue-name data-handler {:auto-ack true})))



(defn publish
  "Publish a message to a particular exchange and topic performing serialisation according to message type"
  [ch exchange-name topic-name job-message]
  (lexchange/declare ch exchange-name "direct")
  (lqueue/declare ch topic-name {:exclusive false :auto-delete false})
  (lqueue/bind    ch topic-name exchange-name {:routing-key topic-name})
  (let [serialiser (pserialisation/make-serialiser (:type job-message))
        media-type (pserialisation/media-type serialiser)
        headers (assoc (:headers job-message) :content-type media-type)
        content (pserialisation/write serialiser (:content job-message))]
    (lbasic/publish ch exchange-name topic-name content {:content-type "application/json"})))

