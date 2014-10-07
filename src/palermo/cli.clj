(ns palermo.cli
  (:gen-class)
  (:require [palermo.cli.web :as pcliweb]
            [palermo.cli.utils :as pcliutils]))

(defn -main [& args]
  (condp = (first args)
    "web" (pcliweb/start (into-array String (drop 1 args)))
    (pcliutils/to-help "java -jar palermo.jar web" palermo.cli.web/OPTIONS)))
