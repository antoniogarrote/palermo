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
  [channel exchange-name queue-name]
  (loop [messages []
         keys {}
         [next-metadata next-payload] (lbasic/get channel queue-name)]
    (let [message-id (:message-id queue-name)]
      (if (nil? (get keys message-id))
        (do (prabbit/pipe-message channel exchange-name queue-name next-payload next-metadata)
            (recur (conj messages [next-metadata next-payload])
                   (assoc keys message-id true)
                   (lbasic/get channel queue-name)))
        messages))))
