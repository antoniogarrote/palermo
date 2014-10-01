(defproject palermo "0.1.0-SNAPSHOT"
  :description "Palermo, a job processing system built with love"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["test/main/resource"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.novemberain/langohr "3.0.0-rc2"]
                 ;; json serialisation
                 [org.clojure/data.json "0.2.5"]
                 ;; java serialisation using jboss
                 [jboss/jboss-serialization "4.2.2.GA"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [trove/trove "1.0.2"]
                 ;; jetty
                 [org.eclipse.jetty/jetty-jsp "9.2.3.v20140905"]
                 [org.eclipse.jetty/jetty-webapp "9.2.3.v20140905"]
                 [org.eclipse.jetty/jetty-server "9.2.3.v20140905"]])
