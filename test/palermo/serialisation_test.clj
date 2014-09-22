(ns palermo.serialisation_test
  (:require [clojure.test :refer :all]
            [palermo.serialisation :refer :all]))

(deftest test-use-json-serialisation
  (testing "Should be possible to use the provided json serialiser/deserialiser"
    (let [json-serialiser-1 (make-serialiser :json)
          json-serialiser-2 (make-serialiser "application/json")]
      (is (= (read-data json-serialiser-2 
                        (write-data json-serialiser-1 {"a" 1})) 
             {"a" 1})))))


(deftest test-registration-serialisers
  (testing "Should be possible to register new serialisers/deserialisers"

    (deftype TestSerialiser []
      palermo.serialisation.JobSerialiser
      (write-data [s message] {:message message})
      (read-data [s message] (:message message))
      (media-type [s] "application/bogus"))
    (defmethod make-serialiser :test [type-key]
      (make-serialiser "application/bogus"))
    (defmethod make-serialiser "application/bogus" [type-key]
      (palermo.serialisation_test.TestSerialiser.))

    (let [test-serialiser-1 (make-serialiser :test)
          test-serialiser-2 (make-serialiser "application/bogus")]
      (is (= (read-data test-serialiser-2
                        (write-data test-serialiser-1 {"a" 1}))
             {"a" 1})))))
