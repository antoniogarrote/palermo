(ns lampedusa.core
  (:gen-class)
  (:import [java.util Random])
  (:require [palermo.core :as palermo]))



(defn -main [& args]
  (loop []
    (let [timeout (.nextInt (new Random) 20000)]
      (Thread/sleep timeout)
      (println "Enqueueing a new clojure job...")
      (palermo/enqueue (palermo/palermo) 
                       "clojure" 
                       lampedusa.jobs.LampedusaClojureJob 
                       (java.util.Date.)))
    (recur)))
