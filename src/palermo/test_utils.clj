(ns palermo.test_utils
  (:require [palermo.rabbit :refer :all]
            [palermo.core :refer [defpalermojob]]))

(def ^:dynamic *test-rabbit*
  {:host "localhost" 
   :port 5672
   :username "guest"
   :password "guest"
   :vhost "/"})

(println (str  "\n\n\n*** USING TEST RABBITMQ SERVER ***\n"
               *test-rabbit*
               "\n\n\n"))

(defn rabbit-test []
  (connect (:host *test-rabbit*)
           (:port *test-rabbit*)
           (:username *test-rabbit*)
           (:password *test-rabbit*)
           (:vhost *test-rabbit*)))


(defpalermojob TestMessageJob
  (process [j args] args))

(def test-messages (atom []))

(defn add-test-message [message]
  (swap! test-messages conj message))

(defpalermojob TestMessageAccJob
  (process [j args] 
    (add-test-message args)))

(defpalermojob TestErrorMessageJob
  (process [j args] 
    (throw (Exception. "Test error"))))

(defpalermojob TestPipeJob 
  (process [j args] args))

(defpalermojob TestLong 
  (process [job timeout]
    (println "SLEEPING...")
    (Thread/sleep timeout)
    (println "BACK!")))
