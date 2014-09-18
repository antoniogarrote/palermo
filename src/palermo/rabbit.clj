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

(defn close-channel
  "Closes a RabbitMQ channel"
  [channel]
  (rmq/close channel))

(defn cancel-subscriber
  "Cancels a subscriber provided a channel and consumer-tag"
  [channel consumer-tag]
  (lbasic/cancel channel consumer-tag))

(defn consume-job-messages
  "Starts a consumer attached to a queue and bound to a a certain topic"
  ([ch exchange-name queue-name handler]
     (consume-job-messages ch exchange-name queue-name handler #(println %)))
  ([ch exchange-name queue-name handler error-handler]
     (let [topic-name queue-name
           data-handler    (fn [ch metadata ^bytes payload]
                             (try  
                               (let [media-type (:content-type metadata)
                                     message-id (:message-id metadata)
                                     headers (clojure.walk/keywordize-keys
                                              (into {} (for [[k v] (:headers metadata)]
                                                         [k (if(= (class v) 
                                                                  com.rabbitmq.client.impl.LongStringHelper$ByteArrayLongString)
                                                              (.toString v)
                                                              v)])))
                                     job-class (java.lang.Class/forName (:job-class headers))
                                     headers (dissoc headers :job-class)
                                     headers (if (nil? message-id)
                                               headers
                                               (assoc headers :id message-id))
                                     serialiser (pserialisation/make-serialiser media-type)
                                     content (pserialisation/read serialiser payload)
                                     job-message (pjob/make-job-message media-type job-class 
                                                                        content headers)]
                                 (handler job-message))
                               (catch Exception e
                                 (error-handler e metadata payload))))]
       (lexchange/declare ch exchange-name "direct")
       (lqueue/declare ch queue-name {:exclusive false :auto-delete false})
       (lqueue/bind    ch queue-name exchange-name {:routing-key topic-name})
       (lconsumers/subscribe ch queue-name data-handler {:auto-ack true}))))


(defn publish-job-messages
  "Publish a message to a particular exchange and topic performing serialisation according to message type"
  [ch exchange-name topic-name job-message]
  (lexchange/declare ch exchange-name "direct")
  (lqueue/declare ch topic-name {:exclusive false :auto-delete false})
  (lqueue/bind    ch topic-name exchange-name {:routing-key topic-name})
  (let [serialiser (pserialisation/make-serialiser (:type job-message))
        media-type (pserialisation/media-type serialiser)
        job-class (if (string? (:job-class job-message)) 
                    (:job-class job-message) 
                    (.getName (:job-class job-message)))
        message-id (or (:id (:headers job-message)) (str (java.util.UUID/randomUUID)))
        headers (assoc (:headers job-message) :job-class job-class)
        headers (assoc headers :id message-id)
        headers (clojure.walk/stringify-keys headers)
        content (pserialisation/write serialiser (:content job-message))]
    (lbasic/publish ch exchange-name topic-name content {:content-type "application/json"
                                                         :persistent true
                                                         :message-id message-id
                                                         :headers headers})))
