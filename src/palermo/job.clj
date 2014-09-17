(ns palermo.job)

(defn job-message
  "Build a message for a job containing the payload and required meta-data"
  ([type content]
     (job-message type content {}))
  ([type content headers]
     {:type type
      :content content
      :headers headers})
  ([type content key & headers]
     (job-message type content (cons key headers))))
