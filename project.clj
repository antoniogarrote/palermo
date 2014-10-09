(defproject palermo "0.3.1-SNAPSHOT"
  :description "Palermo, a job processing system built with love"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :javac-options     ["-target" "1.6" "-source" "1.6"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.novemberain/langohr "3.0.0-rc3"]
                 ;; json serialisation
                 [org.clojure/data.json "0.2.5"]
                 ;; java serialisation using jboss
                 [jboss/jboss-serialization "4.2.2.GA"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [trove/trove "1.0.2"]
                 ;; cli
                 [commons-cli/commons-cli "1.2"]
                 ;; web
                 [ring/ring-jetty-adapter "1.3.1"]
                 [compojure "1.2.0"]
                 [hiccup "1.0.5"]
                 ]

  :main palermo.cli

  ; ring
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler palermo.web/start-dev}
  :aot [palermo.server palermo.test_utils]
  )
