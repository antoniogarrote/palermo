(ns palermo.server
  (:require [palermo.rabbit        :as prabbit]
            [palermo.job           :as pjob]
            [palermo.worker        :as pworker]
            [palermo.introspection :as pintrospection])
  (:gen-class
    :init init
    :name palermo.Server
    :constructors {[String Integer String String String String] []
                   [String Integer String] []
                   [String] []
                   [] []}
    :methods [
              [show [] java.util.HashMap]
              [enqueue [String java.lang.Class Object] void]
              [startWorker ["[Ljava.lang.String;"] String]
              [startWorker ["[Ljava.lang.String;" Integer] String]
              [stopWorker [String] void]
              [workers [] "[Ljava.lang.String;"]
              [setSerialization [String] void]
              [getSerialization [] String]
              [getQueuesInfo [] java.util.HashMap]
              [getWorkersInfo [] java.util.HashMap]
              [getQueueJobs [String] java.util.ArrayList]
              [retryJob [String] void]
              [retryAllFailedJobs [] void]
              [purgeQueue [String] void]
             ]
    :state state))

(defn to-java-nested-hashes
  "Transforms a collection of nested Clojure PersistentArrayMaps into java HashMaps"
  [m]
  (let [mapped (map (fn [[k,v]] 
                      (do
                        (if (map? v)
                          [k (to-java-nested-hashes (clojure.walk/stringify-keys v))]
                          (if (coll? v)
                            [k (java.util.ArrayList. (map to-java-nested-hashes v))]
                            [k v]))))
                    m)]
    (java.util.HashMap. (apply hash-map (apply concat mapped)))))

(defn connect 
  "Establish a connection to RabbitMQ"
  ([{:keys [host port username password vhost]}]
     (prabbit/connect host port username password vhost))
  ([{:keys [host port username password vhost]} max-threads]
     (prabbit/connect host port username password vhost max-threads)))

(defn -init 
  ([]
     [[] (let [connection (connect {:host "localhost" :port 5672 :username "guest" :password "guest" :vhost "/"})]
           (atom {:connection connection
                  :host "localhost"
                  :port 5672
                  :vhost "/"
                  :exchange "palermo"
                  :username "guest"
                  :password "guest"
                  :serialization "application/x-java-serialized-object"
                  :consumers {}}))])
  ([exchange] 
     [[] (let [connection (connect {:host "localhost" :port 5672 :username "guest" :password "guest" :vhost "/"})]
           (atom {:connection connection
                  :vhost "/"
                  :host "localhost"
                  :port 5672
                  :exchange exchange
                  :username "guest"
                  :password "guest"
                  :serialization "application/x-java-serialized-object"
                  :consumers {}}))])
  ([host port username password exchange vhost]
     [[] (let [connection (connect {:host host :port port :username username :password password :vhost vhost})]
           (atom {:connection connection
                  :vhost vhost
                  :host host
                  :port port
                  :exchange exchange
                  :username username
                  :password password
                  :serialization "application/x-java-serialized-object"
                  :consumers {}}))])
  ([host port exchange]
     [[] (let [connection (connect {:host host :port port :username "guest" :password "guest" :vhost "/"})]
           (atom {:connection connection
                  :vhost "/"
                  :host host
                  :port port
                  :exchange exchange
                  :username "guest"
                  :password "guest"
                  :serialization "application/x-java-serialized-object"
                  :consumers {}}))]))

(defn -setSerialization [this serialization]
  (swap! (.state this) assoc :serialization serialization))

(defn -getSerialization [this]
  (:serialization (deref (.state this))))

(defn -enqueue [this queue job-class arguments]
  (let [serialization-type (:serialization (deref (.state this)))
        exchange-name (:exchange (deref (.state this)))
        id (str (java.util.UUID/randomUUID))
        headers {:id id
                 :created-at (pjob/unix-timestamp)
                 :queue queue}
        job-message (pjob/make-job-message serialization-type job-class arguments headers)
        ch (prabbit/channel (:connection (deref (.state this))))]
    (prabbit/publish-job-messages ch exchange-name queue job-message)
    (prabbit/close-channel ch)))

(defn -startWorker [this queues]
  (let [worker-id (str (java.util.UUID/randomUUID))
        channel (prabbit/channel (:connection (deref (.state this))))
        _ (prabbit/qos-channel channel 1)
        exchange-name (:exchange (deref (.state this)))
        worker-tags (pworker/start-worker channel exchange-name queues)
        old-workers (:workers (deref (.state this)))]
    (swap! (.state this) assoc :workers (assoc old-workers worker-id {:channel channel
                                                                      :tags worker-tags}))
    worker-id))

(defn -startWorker [this queues max-threads]
  (let [host (:host (deref (.state this)))
        port (:port (deref (.state this)))
        username (:username (deref (.state this)))
        password (:password (deref (.state this)))
        vhost (:vhost (deref (.state this)))
        connection (connect {:host host :port port :username username :password password :vhost vhost} max-threads)
        worker-id (str (java.util.UUID/randomUUID))
        channel (prabbit/channel connection)
        _ (prabbit/qos-channel channel 1)
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
  (to-java-nested-hashes (clojure.walk/stringify-keys (deref (.state this)))))


(defn -getQueuesInfo [this]
  (let [info (pintrospection/queues-for-exchange (:exchange (deref (.state this)))
                                                 (:vhost (deref (.state this))))]
    (to-java-nested-hashes (clojure.walk/stringify-keys info))))

(defn -getWorkersInfo [this]
  (let [info (pintrospection/consumers-for-exchange (:exchange (deref (.state this)))
                                                    (:vhost (deref (.state this))))]
    (to-java-nested-hashes (clojure.walk/stringify-keys info))))

(defn -getQueueJobs [this queue-name]
  (let [channel (prabbit/channel (:connection (deref (.state this))))
        jobs (pintrospection/jobs-for-queue
               channel
               (:exchange (deref (.state this)))
               queue-name)
        jobs (if (and (= (count jobs) 1)
                      (= 0 (alength (:payload (first jobs)))))
               []
               jobs)
        jobs (map (comp to-java-nested-hashes clojure.walk/stringify-keys) jobs)]
    (prabbit/close-channel channel)
    (java.util.ArrayList. jobs)))


(defn -retryJob [this message-id]
  (let [channel (prabbit/channel (:connection (deref (.state this))))
        exchange (:exchange (deref (.state this)))
        jobs (pintrospection/jobs-for-queue
               channel
               exchange
               pworker/FAILED_QUEUE
               #(not= (:message-id %) message-id))
        jobs (if (and (= (count jobs) 1)
                      (= 0 (alength (:payload (first jobs)))))
               []
               jobs)
        extracted (filter (fn [{:keys [metadata payload]}] (= (:message-id metadata) message-id)) jobs)]
    (doseq [{:keys [metadata payload]} extracted]
      (let [headers (:headers metadata)
            retries (get headers "retries")
            retries (inc retries)
            metadata (assoc metadata :headers headers)
            queue (.toString (get headers "queue"))]
        (prabbit/pipe-message channel exchange queue payload metadata)))
    (prabbit/close-channel channel)))

(defn -retryAllFailedJobs [this]
  (let [channel (prabbit/channel (:connection (deref (.state this))))
        exchange (:exchange (deref (.state this)))
        jobs (pintrospection/jobs-for-queue
               channel
               exchange
               pworker/FAILED_QUEUE
               (fn [_] false))
        jobs (if (and (= (count jobs) 1)
                      (= 0 (alength (:payload (first jobs)))))
               []
               jobs)]
    (doseq [{:keys [metadata payload]} jobs]
      (let [headers (:headers metadata)
            retries (get headers "retries")
            retries (inc retries)
            metadata (assoc metadata :headers headers)
            queue (.toString (get headers "queue"))]
        (prabbit/pipe-message channel exchange queue payload metadata)))
    (prabbit/close-channel channel)))


(defn -purgeQueue [this queue-name]
  (let [channel (prabbit/channel (:connection (deref (.state this))))]
    (prabbit/purge-queue channel queue-name)))
