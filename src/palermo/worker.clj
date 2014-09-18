(ns palermo.worker
  (:require [palermo.job :as pjob]
            [palermo.rabbit :as prabbit]))

;; constant name for failed queue
(def FAILED_QUEUE "failed")

(defn instantiate
  "Instantiates a new instance of a java class using the default
   constructor"
  [class-name]
  (let [klass (if (string? class-name)
                (java.lang.Class/forName class-name)
                class-name)
        constructor (.getConstructor klass (into-array Class []))]
    (.newInstance constructor (into-array Object []))))

(defn worker-handler
  "Handles a new job message received from RabbitQM"
  [{:keys [type job-class content  headers]}]
  (let [job (instantiate job-class)]
    (pjob/process job content)))

(defn worker-error-handler
  "Generates an error handler that re-enqueue a failed job into the 
   failed queue"
  [channel exchange-name]
  (fn [exception metadata payload]
    (let [exception-message (.getMessage exception)
          stack-trace (map (fn [trace] (.toString trace))
                           (.getStackTrace exception))
          stack-trace (clojure.string/join "\n" stack-trace)
          type (:content-type metadata)
          headers (:headers metadata)
          job-class (:job-class headers)
          error-job-message (pjob/make-job-message type job-class payload headers)]
      (prabbit/publish-job-messages channel 
                                    exchange-name 
                                    FAILED_QUEUE 
                                    error-job-message))))

(defn start-worker
  "Starts the execution of a new Palermo worker"
  [channel exchange-name queues]
  (let [tags (map 
              (fn [queue]
                (prabbit/consume-job-messages
                 channel
                 exchange-name
                 queue
                 worker-handler
                 (worker-error-handler channel exchange-name)))
              (vec queues))]
    ;; trigger map function
    (doseq [tag tags] tag)
    tags))
