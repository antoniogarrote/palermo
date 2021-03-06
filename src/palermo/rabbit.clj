(ns palermo.rabbit
  (:import [java.util.concurrent Executors])
  (:require [langohr.core :as rmq]
            [langohr.basic :as lbasic]
            [langohr.channel :as lchannel]
            [langohr.exchange :as lexchange]
            [langohr.queue :as lqueue]
            [langohr.consumers :as lconsumers]
            [palermo.serialisation :as pserialisation]
            [palermo.job :as pjob]))

(def WORKER_COUNT (atom 0))

(defn worker-id []
  (let [next-counter (swap! WORKER_COUNT inc)
        process-id (.getName (java.lang.management.ManagementFactory/getRuntimeMXBean))]
    (str "pid:" process-id "/worker-" next-counter)))


(defn connect
  "Connects to the RabbitMQ broker"
  ([host port username password vhost max-threads]
     (let [thread-factory (.getThreadFactory (Executors/newFixedThreadPool (int max-threads)))]
       (rmq/connect {:host host :port port :username username :password password :vhost vhost :thread-factory thread-factory})))
  ([host port username password vhost]
     (rmq/connect {:host host :port port :username username :password password :vhost vhost}))
  ([]
     (rmq/connect)))

(defn close-connection
  "Closes the RabbitMQ connection"
  [connection]
  (.close connection))

(defn channel
  "Opens a Langohr channel using the provided connection"
  [connection]
  (lchannel/open connection))

(defn close-channel
  "Closes a RabbitMQ channel"
  [channel]
  (rmq/close channel))

(defn qos-channel
  "Sets the QOS for a channel"
  [channel prefetch]
  (lbasic/qos channel prefetch))

(defn cancel-subscriber
  "Cancels a subscriber provided a channel and consumer-tag"
  [channel consumer-tag]
  (lbasic/cancel channel consumer-tag))

(defn process-headers
  "Process the incoming RabbitMQ headers to produce a
   key indexed Clojure map"
  [headers]
  (clojure.walk/keywordize-keys
   (into {} (for [[k v] headers]
              [k (if(= (class v) 
                       com.rabbitmq.client.impl.LongStringHelper$ByteArrayLongString)
                   (.toString v)
                   v)]))))

(defn exchange 
  "Declares a new durable direct exchange"
  [channel exchange-name]
  (lexchange/declare channel exchange-name "direct" {:durable true}))

(defn queue
  "Declares a durable queue"
  [channel queue-name]
  (lqueue/declare channel queue-name {:exclusive false :auto-delete false :durable true}))

(defn purge-queue
  "Removes all messages in the provided queue"
  [channel queue-name]
  (lqueue/purge channel queue-name))
(defn pipe-message
  "Redirects a message to another queue"
  [channel exchange-name topic-name payload metadata]
  (exchange channel exchange-name)
  (queue channel topic-name)
  (lqueue/bind    channel topic-name exchange-name {:routing-key topic-name})
  (lbasic/publish channel exchange-name topic-name payload metadata))

(defn ack-message
  "Acknowledges the reception of a message"
  [channel delivery-tag]
  (lbasic/ack channel delivery-tag))


(defn consume-job-messages
  "Starts a consumer attached to a queue and bound to a a certain topic"
  ([ch exchange-name queue-name handler]
     (consume-job-messages ch exchange-name queue-name handler 
                           (fn [e metadata payload]
                             (println e)
                             (println (.getMessage e))
                             (println metadata)
                             (println payload))))
  ([ch exchange-name queue-name handler error-handler]
     (let [topic-name queue-name
           data-handler    (fn [ch metadata ^bytes payload]
                             (do
                               (try  
                                 (let [media-type (:content-type metadata)
                                       message-id (:message-id metadata)
                                       headers (process-headers (:headers metadata))
                                       job-class (java.lang.Class/forName (:job-class headers))
                                       headers (dissoc headers :job-class)
                                       headers (if (nil? message-id)
                                                 headers
                                                 (assoc headers :id message-id))
                                       serialiser (pserialisation/make-serialiser media-type)
                                       content (if (nil? payload)
                                                 nil
                                                 (pserialisation/read-data serialiser payload))
                                       job-message (pjob/make-job-message media-type job-class 
                                                                          content headers)]
                                   (handler job-message))
                                 (catch Exception e
                                   (do
                                     (println (str "EXCEPTION " (.getMessage e)))
                                     (.printStackTrace e)
                                     (error-handler e metadata payload))))
                               (ack-message ch (:delivery-tag metadata))))]
       (exchange ch exchange-name)
       (queue ch queue-name)
       (lqueue/bind    ch queue-name exchange-name {:routing-key topic-name})
       (lconsumers/subscribe ch queue-name data-handler {:auto-ack false 
                                                         :consumer-tag (worker-id)}))))


(defn publish-job-messages
  "Publish a message to a particular exchange and topic performing serialisation according to message type"
  [ch exchange-name topic-name job-message]
  (exchange ch exchange-name)
  (queue ch topic-name)
  (lqueue/bind    ch topic-name exchange-name {:routing-key topic-name})
  (let [serialiser (pserialisation/make-serialiser (:type job-message))
        media-type (pserialisation/media-type serialiser)
        job-class (if (string? (:job-class job-message)) 
                    (:job-class job-message) 
                    (.getName (:job-class job-message)))
        message-id (or (:id (:headers job-message)) (str (java.util.UUID/randomUUID)))
        headers (assoc (:headers job-message) :job-class job-class)
        headers (assoc headers :id message-id)
        headers (assoc headers :queue topic-name)
        headers (assoc headers :preview (pjob/preview-content job-message))
        headers (clojure.walk/stringify-keys headers)
        content (pserialisation/write-data serialiser (:content job-message))]
    (lbasic/publish ch exchange-name topic-name content {:content-type media-type
                                                         :persistent true
                                                         :message-id message-id
                                                         :headers headers
                                                         :destination topic-name})))
