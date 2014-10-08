(ns palermo.cli
  (:gen-class)
  (:require [palermo.cli.web :as pcliweb]
            [palermo.cli.worker :as pworker]
            [palermo.cli.utils :as pcliutils]))

(defn -main [& args]
  (condp = (first args)
    "web"     (pcliweb/start (into-array String (drop 1 args)))
    "worker"  (pworker/start (into-array String (drop 1 args)))
    (do 
      (pcliutils/to-help "web" palermo.cli.web/OPTIONS)
      (pcliutils/to-help "worker" palermo.cli.worker/OPTIONS)
      (System/exit 0))))
