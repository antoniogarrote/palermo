(ns palermo.introspection
  (:require [langohr.http :as lhttp]
            [langohr.basic :as lbasic]
            [palermo.rabbit :as prabbit]))

(defn queues-for-exchange
  "Returns data for all the queueues in an exchange and vhost"
  [exchange-name vhost]
  (let [queue-bindings (lhttp/list-bindings-for-which-exchange-is-the-source vhost exchange-name)
        queue-names (for [queue queue-bindings :when (= (:destination_type queue) "queue")] 
                      (:destination queue))]
    (->> (map (fn [queue-name]
                (let [queue-data (lhttp/get-queue vhost queue-name)]
                  [queue-name {:workers (:consumers queue-data)
                               :jobs (:messages queue-data)}]))
              queue-names)
         (into {}))))


(defn consumers-for-exchange
  "Returns information about the consumers connected to the queues in an exchange"
  [exchange-name vhost]
  (let [queue-bindings (lhttp/list-bindings-for-which-exchange-is-the-source vhost exchange-name)
        queue-names (for [queue queue-bindings :when (= (:destination_type queue) "queue")] 
                      (:destination queue))
        info (map (fn [queue-name]
                    (let [queue-info (langohr.http/get-queue vhost queue-name)
                          consumer-details (:consumer_details queue-info)]
                      [queue-name consumer-details]))
                  queue-names)]
    (apply hash-map (apply concat info))))


(defn jobs-for-queue
  "Returns the list of jobs in queue"
  ([channel exchange-name queue-name should-pipe]
      (loop [messages []
             keys {}
             to-pipe []
             [next-metadata next-payload] (lbasic/get channel queue-name)]
        (if (nil? next-payload)
          ;; no messages
          (do 
            (doseq [[read-metadata read-payload] to-pipe]
              (prabbit/pipe-message channel exchange-name queue-name read-payload read-metadata))
            messages)
          (if (and (= 0 (alength next-payload))
                   (nil? (:headers next-metadata)))
            ;; filter weird nil messges
            (recur messages 
                   keys
                   to-pipe
                   (lbasic/get channel queue-name))
            ;; normal processing
            (let [message-id (:message-id next-metadata)]
              (if (nil? (get keys message-id))
                (recur (conj messages {:metadata next-metadata
                                       :payload next-payload})
                       (assoc keys message-id true)
                       (if (should-pipe next-metadata)
                         (conj to-pipe [next-metadata next-payload])
                         to-pipe)
                       (lbasic/get channel queue-name))
                (do 
                  (doseq [[read-metadata read-payload] to-pipe]
                    (prabbit/pipe-message channel exchange-name queue-name read-payload read-metadata))
                  messages)))))))
  ([channel exchange-name queue-name]
     (jobs-for-queue channel exchange-name queue-name (fn [_] true))))
