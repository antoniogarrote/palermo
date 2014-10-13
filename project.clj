(defproject lampedusa "0.1.0-SNAPSHOT"
  :description "An Palermo job enqueer example"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [palermo "0.3.6-SNAPSHOT"]
                 [congomongo "0.4.4"]]
  :aot [lampedusa.jobs]
  :main lampedusa.core)
