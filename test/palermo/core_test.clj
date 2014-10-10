(ns palermo.core_test
  (:require [clojure.test :refer :all]
            [langohr.exchange :as lexchange]
            [langohr.queue :as lqueue]
            [langohr.channel :as lchannel]
            [palermo.core :refer :all])
  (:use palermo.test_utils))


(deftest test-should-create-a-connection
  (testing "Should create a Palermo connection to RabbitMQ"
    (let [p (palermo *test-rabbit*)]
      (is (not (nil? p)))
      (is (= (get (.show p) "host")
             (:host *test-rabbit*)))
      (disconnect p))))

(deftest test-should-set-get-serialization
  (testing "Should be possible to set and retrieve the current serialization"
    (let [p (palermo *test-rabbit*)]
      (serialization p "application/x-foo")
      (is (= (serialization p)
             "application/x-foo"))
      (disconnect p))))

(deftest test-publish-create-consumer
  (testing "Should be possible to start a worker and send messages to the worker queue"
    (swap! test-messages empty)
    
    (let [test-exchange (str "palermo_test_queue_" (java.util.UUID/randomUUID))
          queue-name (str "palermo_test_queue_" (java.util.UUID/randomUUID))
          options (assoc *test-rabbit* :exchange test-exchange)
          p (palermo options)
          channel (lchannel/open  (get (.show p) "connection"))]
      (enqueue p queue-name palermo.test_utils.TestMessageAccJob {:id 1})
      (start-worker p queue-name)
      (Thread/sleep 3000)
      (is (= (count @test-messages) 1))
      (is (= (first @test-messages) {:id 1}))
      (lexchange/delete channel test-exchange)
      (lqueue/delete channel queue-name)
      (disconnect p))))
