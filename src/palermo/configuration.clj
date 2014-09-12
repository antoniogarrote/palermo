(ns palermo.configuration
  (:use [palermo.configuration.impl :only [load-configuration-file
                                           configuration-to-yaml]]))

(defn gen-clj-configuration-provider
  "Generates a ConfigurationProvider for a provided Clojure configuration file"
  []
  (proxy [io.dropwizard.configuration.ConfigurationSourceProvider]
      []
    (open [path] 
      (let [yaml-configuration (-> path 
                                   load-configuration-file 
                                   configuration-to-yaml)]
        (java.io.ByteArrayInputStream. (.getBytes yaml-configuration))))))

