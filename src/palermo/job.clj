(ns palermo.job)

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

(defprotocol PalermoJob
  (process [j args]))
