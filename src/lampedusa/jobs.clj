(ns lampedusa.jobs
  (:use [palermo.core]
        [somnium.congomongo]))

(def conn
  (make-connection "lampedusa"
                   :host "127.0.0.1"
                   :port 27017))

(defpalermojob LampedusaClojureJob
  (process [j date]
           
           ; Requiring the Clojure namespace
           (require 'lampedusa.jobs) 

           (let [local conn
                 sdf (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm'Z'")]
             (with-mongo local
               (insert! :times {:iso8601 (.format sdf date)
                                :unix (* (.getTime date) 1000)
                                :date date})))))
