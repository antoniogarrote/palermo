(ns palermo.core
  (:import [palermo PalermoServer])
  (:use [palermo.job])
  (:require [palermo.introspection :as pintrospection]))

(defn palermo
  "Creates a new Palermo instance.
   Possible options are host, port, vhost, username, password and exchange."
  ([options]
      (let [host (get options :host "localhost")
            port (get options :port 5672)
            vhost (get options :vhost "/")
            username (get options :username "guest")
            password (get options :password "guest")
            exchange (get options :exchange "palermo")]
        (new PalermoServer host (int port) username password exchange vhost)))
  ([] (palermo {})))

(defn serialization 
  "Sets or retrieves the serialization for this instance of Palermo."
  ([palermo serialization]
     (.setSerialization palermo serialization))
  ([palermo]
     (.getSerialization palermo)))

(defn enqueue
  "Enqueues a new job into the provided palermo queue."
  [palermo queue job-class arguments]
  (.enqueue palermo queue job-class arguments))

(defn start-worker
  "Starts a new worker listening for jobs in the provided queues."
  [palermo queues]
  (.startWorker palermo (into-array String queues)))

(defn workers
  "Returns the list of worker-ids connected to this Palermo instance."
  [palermo]
  (apply list (.workers palermo)))

(defn stop-worker
  "Disconnects the worker identified by the provided worker-id."
  [palermo worker-id]
  (.stopWorker palermo worker-id))

(defn queues-info
  "Retrieves information about the state of the Palermo queues."
  [palermo]
  (let [state (.show palermo)
        exchange (get state "exchange")
        vhost (get state "vhost")]
    (pintrospection/queues-for-exchange exchange vhost)))

(defn workers-info
  "Retrieves information about the state of the Palermo connected workers."
  [palermo]
  (let [state (.show palermo)
        exchange (get state "exchange")
        vhost (get state "vhost")]
    (pintrospection/consumers-for-exchange exchange vhost)))

(defn jobs-in-queue
  "Retrieves information about the jobs in a Palermo queue."
  [palermo queue]
  (.getQueueJobs palermo queue))

(defn retry-failed-job
  "Retries the failed job whose message-id is provided."
  [palermo message-id]
  (.retryJob palermo message-id))

(defn retry-all
  "Retries all the failed jobs."
  [palermo]
  (.retryAllFailedJobs palermo))

(defn purge-queue
  "Removes all pending jobs from a Palermo queue."
  [palermo queue]
  (.purgeQueue palermo queue))

(defmacro defpalermojob [job-name process-form]
  `(deftype ~job-name []
     palermo.job.PalermoJob
     ~process-form))
