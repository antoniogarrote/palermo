(ns palermo.rabbit_test
  (:require [clojure.test :refer :all]
            [langohr.exchange :as lexchange]
            [langohr.queue :as lqueue]
            [palermo.rabbit :refer :all]
            [palermo.serialisation :refer :all]
            [palermo.test_utils :refer :all]
            [palermo.job :as pjob]))


(deftest test-connect
  (testing "Should open a rabbitmq connection"
    (let [connection (rabbit-test)]
      (is (not (nil? connection)))
      (is (.isOpen connection))
      (.close connection))))


(deftest test-channel
  (testing "Should get an open channel from an open connection"
    (let [connection (rabbit-test)
          channel (channel connection)]
      (is (.isOpen connection))
      (is (.isOpen channel))
      (.close channel)
      (.close connection))))

(deftest test-close-channel
  (testing "Should be possible to close an open channel"
    (let [connection (rabbit-test)
          ch (channel connection)]
      (is (.isOpen connection))
      (is (.isOpen ch))
      (close-channel ch)
      (is (not (.isOpen ch)))
      (.close connection))))

(deftest test-process-headers
  (testing "Should be possible to transform a hash of options with string keys into a keyed map matching the origin headers map"
    (let [to-test {"a" 1 "b" "2"}
          result {:a 1 :b "2"}]
      (is (= (process-headers to-test)
             result)))))

(deftest test-publish-consume
  (testing "Should be possible to publish and consume correct messages"
    
    (deftype TestMessageJob []
      palermo.job.PalermoJob
      (process [j args] args))

    (let [consumed (atom [])
          connection (rabbit-test)
          ch (channel connection)
          test-exchange-1 (str "palermo_test_" (java.util.UUID/randomUUID))
          test-queue-1 "test_queue_1"
          test-exchange-2 (str "palermo_test_" (java.util.UUID/randomUUID))
          test-queue-2 "test_queue_2"
          message (pjob/make-job-message :json palermo.rabbit_test.TestMessageJob 1)]
      ;; first publish then consume
      (publish-job-messages ch test-exchange-1 test-queue-1 message)
      (Thread/sleep 3000)
      (consume-job-messages ch test-exchange-1 test-queue-1
                            (fn [message]
                              (swap! consumed conj (:content message))))
      ;; first consume, then publish
      (consume-job-messages ch test-exchange-2 test-queue-2
                            (fn [message]
                              (swap! consumed conj (:content message))))
      (Thread/sleep 3000)      
      (publish-job-messages ch test-exchange-2 test-queue-2 message)
      (Thread/sleep 3000)      
      ;; both results received?
      (is (= @consumed [1 1]))
      ;; clean up
      (lexchange/delete ch test-exchange-1)
      (lexchange/delete ch test-exchange-2)
      (lqueue/delete ch test-queue-1)
      (lqueue/delete ch test-queue-2)
      (.close ch)
      (.close connection))))

(deftest test-publish-failure
  (testing "Should be possible to catch errors in the processing of messages providing the right error handler function"
    
    (deftype TestErrorJob []
      palermo.job.PalermoJob
      (process [j args] (throw (new Exception "Error processing test message"))))

    (let [consumed (atom [])
          failed (atom [])
          connection (rabbit-test)
          ch (channel connection)
          test-exchange (str "palermo_test_" (java.util.UUID/randomUUID))
          test-queue "test_queue"
          message (pjob/make-job-message :json palermo.rabbit_test.TestErrorJob 1)]
      (publish-job-messages ch test-exchange test-queue message)
      (Thread/sleep 3000)
      (consume-job-messages ch test-exchange test-queue
                            (fn [message] 
                              (pjob/process (new palermo.rabbit_test.TestErrorJob) 
                                            (:content message))
                              (swap! consumed (:content message)))
                            (fn [exception metadata payload]
                              (swap! failed conj [exception metadata payload])))
      (Thread/sleep 3000)
      (is (= 0 (count @consumed)))
      (is (= 1 (count @failed)))
      (lexchange/delete ch test-exchange)
      (lqueue/delete ch test-queue)
      (.close ch)
      (.close connection))))

(deftest test-pipe-message
  (testing "Should be possible to pipe a message from one queue to another queue"

    (deftype TestPipeJob []
      palermo.job.PalermoJob
      (process [j args] args))

    (let [consumed (atom [])
          connection (rabbit-test)
          ch (channel connection)
          test-exchange (str "palermo_test_" (java.util.UUID/randomUUID))
          pipe-queue "test_piped"
          serialiser (make-serialiser :json)
          message "\"hello piped\""
          headers {:content-type "application/json"
                   :persitent true
                   :headers {"job-class" "palermo.rabbit_test.TestPipeJob"}}]
      (pipe-message ch test-exchange pipe-queue message headers)      
      (Thread/sleep 3000)
      (consume-job-messages ch test-exchange pipe-queue
                            (fn [message]
                              (swap! consumed conj message)))
      (Thread/sleep 3000)
      (is (= (-> @consumed first :content) "hello piped"))
      (lexchange/delete ch test-exchange)
      (lqueue/delete ch pipe-queue)
      (.close ch)
      (.close connection))))
