(ns palermo.configuration.impl-test
  (:require [clojure.test :refer :all]
            [palermo.configuration.impl :refer :all]
            [clj-yaml.core :as yaml]))

(deftest test-load-configuration-file
  (testing "Should be possible to load a configuration file containing nested hashes"
    (is (= (load-configuration-file "test/main/resources/config.clj")
           {:server {:port 9090 :host "localhost"}
            :tests true}))))

(deftest test-configuration-to-yaml
  (testing "Should transform a configuration file into an equivalent yml string"
    (let [config (load-configuration-file "test/main/resources/config.clj")
          yml-config (configuration-to-yaml 
                                   config)]
      (is (= config (yaml/parse-string yml-config))))))
