(ns palermo.bootstrap)

(defn run-function
  [fn yaml-file]
  (let [application (proxy [io.dropwizard.Application]
                        []
                      (initialize [bootstrap]
                        (println "bootstraping"))
                      (run [configuration environment]
                        (println "running")
                        (fn)))
        bootstrap (io.dropwizard.setup.Bootstrap. application)]
    (.addCommand bootstrap (io.dropwizard.cli.ServerCommand. application))
    (.addCommand bootstrap (io.dropwizard.cli.CheckCommand. application))
    (.initialize application bootstrap)
    (let [jarLocation (io.dropwizard.util.JarLocation. 
                       (.getClass application))
          cli (io.dropwizard.cli.Cli. jarLocation 
                                      bootstrap
                                      System/out
                                      System/err)]
      (.run cli yaml-file))))
