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
