(ns palermo.cli.worker
  (:use [palermo.cli.utils])
  (:import [palermo Server]))

(def OPTIONS (make-options
              (make-value-option "host" "Host of the RabbitMQ server, defaults to localhost" "HOST" "localhost")
              (make-value-option "port" "Port of the RabbitMQ server, defaults to 5672" "PORT" "5672")
              (make-value-option "username" "Username for the RabbitMQ server, defaults to guest" "USERNAME" "guest")
              (make-value-option "password" "Password for the RabbitMQ server, defaults to guest" "PASSWORD" "guest")
              (make-value-option "vhost" "Virtual Host for the RabbitMQ server, defaults to " "VHOST" "/")
              (make-value-option "exchange" "RabitMQ exchange for Palermo, defaults to palermo" "EXCHANGE" "palermo")
              (make-multi-value-option "queues", "Comma separated list of queues this worker will connect to, defaults to jobs", "QUEUES", "jobs")
              (make-value-option "threads", "Number of worker threads accepting jobs in this worker", "THREADS", "10")
              ))
(defn start [args]
  (let [parser (new org.apache.commons.cli.BasicParser)
        cmd (.parse parser OPTIONS args false)
        host (value-for cmd "host")
        port (Integer/parseInt (value-for cmd "port"))
        username (value-for cmd "username")
        password (value-for cmd "password")
        exchange (value-for cmd "exchange")
        vhost (value-for cmd "vhost")
        queues  (.split (value-for cmd "queues"), ",")
        threads (Integer/parseInt (value-for cmd "threads")) 
        palermo (new Server host port username password exchange vhost)]
    (.startWorker palermo queues threads)))


