(ns palermo.job)

(definterface PalermoJob
  (^void process [^Object args] 
         "Processing logic for the job receiving the incoming arguments"))


(defn unix-timestamp
  "Returns the UNIX timestamp for the current time"
  []
  (long (/ (.getTime (java.util.Date.)) 1000)))

(defn from-unix-timestamp
  "Returns a Date object for the received UNIX timestamp"
  [timestamp]
  (java.util.Date. (* timestamp 1000)))

(defn make-job-message
  "Build a message for a job containing the payload and required meta-data"
  ([type job-class content]
     (make-job-message type job-class content {}))
  ([type job-class content headers]
     {:type type
      :job-class job-class
      :content content
      :headers headers})
  ([type job-class content key & headers]
     (make-job-message type job-class content 
                       (apply hash-map (cons key headers)))))
