(ns palermo.configuration-test
  (:require [clojure.test :refer :all]
            [palermo.configuration :refer :all]
            [palermo.configuration.impl :refer :all]))


(deftest test-gen-clj-configuration-provider
  (testing "Should generate a ConfigurationProvider class wrapping a Clojure configuration file"
    (let [provider (gen-clj-configuration-provider)
          configuration-path "test/main/resources/config.clj"
          yaml-configuration (.open provider configuration-path)]
      (is (= (slurp yaml-configuration) 
             (-> configuration-path 
                 load-configuration-file
                 configuration-to-yaml))))))

