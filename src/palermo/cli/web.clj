(ns palermo.cli.web
  (:use [palermo.cli.utils])
  (:require [palermo.web :as pweb]))

(def OPTIONS (make-options
               (make-value-option "host" "Host of the RabbitMQ server, defaults to localhost" "HOST" "localhost")
               (make-value-option "port" "Port of the RabbitMQ server, defaults to 5672" "PORT" "5672")
               (make-value-option "username" "Username for the RabbitMQ server, defaults to guest" "USERNAME" "guest")
               (make-value-option "password" "Password for the RabbitMQ server, defaults to guest" "PASSWORD" "guest")
               (make-value-option "vhost" "Virtual Host for the RabbitMQ server, defaults to " "VHOST" "/")
               (make-value-option "exchange" "RabitMQ exchange for Palermo, defaults to palermo" "EXCHANGE" "palermo")
               (make-value-option "webport" "Port where the Palermo web interface will be waiting for connections, defaults to 3000" "WEBPORT" "3000")))

(defn start [args]
  (let [parser (new org.apache.commons.cli.BasicParser)
        cmd (.parse parser OPTIONS args false)
        host (value-for cmd "host")
        port (Integer/parseInt (value-for cmd "port"))
        username (value-for cmd "username")
        password (value-for cmd "password")
        exchange (value-for cmd "exchange")
        vhost (value-for cmd "vhost")
        webport (Integer/parseInt (value-for cmd "webport"))]
    (pweb/start webport host port username password vhost exchange)))
