(ns palermo.job_test
  (:require [clojure.test :refer :all]
            [palermo.job :refer :all]))

(deftest test-definition-of-jobs
  (testing "Should be possible to define new types of jobs based on the PalermoJob protocol"
    (deftype TestJob []
      PalermoJob
      (process [j args] {:args args}))
    (let [job (palermo.job_test.TestJob.)]
      (is (= (process job [1 2 3])
             {:args [1 2 3]})))))

(deftest test-make-job-message
  (testing "Should be possible to create job messages"
    (let [message (make-job-message :json String "test" {:a 1})]
      (is (= (:type message) :json))
      (is (= (:job-class message) String))
      (is (= (:content message) "test"))
      (is (= (:headers message) {:a 1})))
    (let [message (make-job-message :json String "test")]
      (is (= (:type message) :json))
      (is (= (:job-class message) String))
      (is (= (:content message) "test"))
      (is (= (:headers message) {})))
    (let [message (make-job-message :json String "test" :a 1)]
      (is (= (:type message) :json))
      (is (= (:job-class message) String))
      (is (= (:content message) "test"))
      (is (= (:headers message) {:a 1})))))
