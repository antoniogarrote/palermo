(ns palermo.serialisation
  (:require [clojure.data.json :as json]))

(defprotocol JobSerialiser
  (write-data [s message])
  (read-data [s message])
  (media-type [s]))


(deftype JsonJobSerialiser []
  palermo.serialisation.JobSerialiser
  (write-data [s message] (json/write-str message))
  (read-data [s message] (json/read-str (String. message)))
  (media-type [s] "application/json"))

;; JSON

(defmulti make-serialiser identity)

(defmethod make-serialiser :json [type-key]
  (make-serialiser "application/json"))

(defmethod make-serialiser "application/json" [type-key]
  (JsonJobSerialiser.))

(defmethod make-serialiser :default [type-key]
  (throw (Exception. (str "Cannot find serialiser for media type " type-key))))


;; Java - JBoss

(deftype JavaJbossJobSerialiser []
  palermo.serialisation.JobSerialiser
  (write-data [s message] (let [^java.io.ByteArrayOutputStream bos (java.io.ByteArrayOutputStream.)
                                 ^org.jboss.serial.io.JBossObjectOutputStream oos (org.jboss.serial.io.JBossObjectOutputStream. bos)]
                             (.writeObject oos message)
                             (.close oos)
                             (.toByteArray bos)))
  (read-data [s message] (let [^java.io.ByteArrayOutputStream bis (java.io.ByteArrayInputStream. (if (string? message)
                                                                                                   (.getBytes message)
                                                                                                   message))
                               ^org.jboss.serial.io.JBossObjectInputStream ois (org.jboss.serial.io.JBossObjectInputStream. bis)
                               ^java.lang.Object obj (.readObject ois)]
                           (.close ois)
                           obj))
  (media-type [s] "application/x-java-serialized-object"))

(defmethod make-serialiser :java [type-key]
  (make-serialiser "application/x-java-serialized-object"))

(defmethod make-serialiser :jboss [type-key]
  (make-serialiser "application/x-java-serialized-object"))

(defmethod make-serialiser "application/x-java-serialized-object" [type-key]
  (JavaJbossJobSerialiser.))
