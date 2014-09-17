(ns palermo.serialisation
  (:require [clojure.data.json :as json]))

(defprotocol JobSerialiser
  (write [s message])
  (read [s message])
  (media-type [s]))

(deftype JsonJobSerialiser []
  palermo.serialisation.JobSerialiser
  (write [s message] (json/write-str message))
  (read [s message] (json/read-str (String. message)))
  (media-type [s] "application/json"))

(defmulti make-serialiser identity)

(defmethod make-serialiser :json [type-key]
  (make-serialiser "application/json"))

(defmethod make-serialiser "application/json" [type-key]
  (JsonJobSerialiser.))

(defmethod make-serialiser :default [type-key]
  (throw (Exception. (str "Cannot find serialiser for media type " type-key))))
