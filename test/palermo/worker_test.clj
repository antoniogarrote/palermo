(ns palermo.worker_test
  (:require [clojure.test :refer :all]
            [langohr.exchange :as lexchange]
            [langohr.queue :as lqueue]
            [palermo.worker :refer :all]
            [palermo.rabbit :refer :all]
            [palermo.job :refer :all]
            [palermo.test_utils :refer :all]))


(def test-messages (atom []))

(defn add-test-message [message]
  (swap! test-messages conj message))

(deftype TestMessageJob []
  palermo.job.PalermoJob
  (process [j args] 
    (add-test-message args)))

(deftype TestErrorMessageJob []
  palermo.job.PalermoJob
  (process [j args] 
    (throw (Exception. "Test error"))))


(deftest test-start-worker
  (testing "Should be possible to start a worker that will consume messages from a queue"

    (swap! test-messages empty)
    
    (let [connection (rabbit-test)
          channel (channel connection)
          test-exchange-1 (str "palermo_test_" (java.util.UUID/randomUUID))
          queue-name (str "palermo_test_queue_" (java.util.UUID/randomUUID))
          tags (start-worker channel test-exchange-1 [queue-name])]
      (is (= 1 (count tags)))
      (publish-job-messages
       channel
       test-exchange-1
       queue-name
       (make-job-message :json palermo.worker_test.TestMessageJob "hey" {:id "1"}))
      (Thread/sleep 3000)
      (is (= (count @test-messages) 1))
      (is (= (first @test-messages) "hey"))
      (lexchange/delete channel test-exchange-1)
      (lqueue/delete channel queue-name)
      (.close channel)
      (.close connection))))

(deftest test-multi-queues
  (testing "Should be possible to start a worker that will consume messages from multiple queues"

    (swap! test-messages empty)
    
    (let [connection (rabbit-test)
          channel (channel connection)
          test-exchange-1 (str "palermo_test_" (java.util.UUID/randomUUID))
          queue-name-a (str "palermo_test_queue_a_" (java.util.UUID/randomUUID))
          queue-name-b (str "palermo_test_queue_b_" (java.util.UUID/randomUUID))
          tags (start-worker channel test-exchange-1 [queue-name-a queue-name-b])]
      (is (= 2 (count tags)))
      ; first message
      (publish-job-messages
       channel
       test-exchange-1
       queue-name-a
       (make-job-message :json palermo.worker_test.TestMessageJob "hey a" {:id "1"}))
      (Thread/sleep 3000)
      ; second message
      (publish-job-messages
       channel
       test-exchange-1
       queue-name-b
       (make-job-message :json palermo.worker_test.TestMessageJob "hey b" {:id "2"}))     
      (Thread/sleep 3000)
      (is (= (count @test-messages) 2))
      (is (= (first  @test-messages) "hey a"))
      (is (= (second @test-messages) "hey b"))
      (lexchange/delete channel test-exchange-1)
      (lqueue/delete channel queue-name-a)
      (lqueue/delete channel queue-name-b)
      (.close channel)
      (.close connection))))


(deftest test-error-handler
  (testing "Should re-enqueue failed messages in the failed queue"

    (swap! test-messages empty)

    (let [connection (rabbit-test)
          channel (channel connection)
          test-exchange-1 (str "palermo_test_" (java.util.UUID/randomUUID))
          queue-name (str "palermo_test_queue_" (java.util.UUID/randomUUID))
          tags (start-worker channel test-exchange-1 [queue-name])]

      (lqueue/purge channel FAILED_QUEUE)

      (is (= (count tags)) 1)

      (publish-job-messages
       channel
       test-exchange-1
       queue-name
       (make-job-message :json palermo.worker_test.TestErrorMessageJob "hey" {:id "1"}))
      (Thread/sleep 3000)
      (is (= (count @test-messages) 0))
      (is (= 0 (lqueue/message-count channel queue-name)))
      (is (= 1 (lqueue/message-count channel FAILED_QUEUE)))
      (lexchange/delete channel test-exchange-1)
      (lqueue/delete channel queue-name)
      (.close channel)
      (.close connection))))
