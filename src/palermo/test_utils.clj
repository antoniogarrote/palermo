(ns palermo.test_utils
  (:require [palermo.rabbit :refer :all]))

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


(deftype TestMessageJob []
  palermo.job.PalermoJob
  (process [j args] args))

(def test-messages (atom []))

(defn add-test-message [message]
  (swap! test-messages conj message))

(deftype TestMessageAccJob []
  palermo.job.PalermoJob
  (process [j args] 
    (add-test-message args)))

(deftype TestErrorMessageJob []
  palermo.job.PalermoJob
  (process [j args] 
    (throw (Exception. "Test error"))))

(deftype TestPipeJob []
  palermo.job.PalermoJob
  (process [j args] args))

(deftype TestLong []
    palermo.job.PalermoJob
  (process [job timeout]
    (println "SLEEPING...")
    (Thread/sleep timeout)
    (println "BACK!")))
