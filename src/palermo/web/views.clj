(ns palermo.web.views
  (:require [hiccup.page :as h]
            [hiccup.form :as f]
            [palermo.worker :as pworker]))

;; layout

(defn layout [host port exchange & body]
  (h/html5
   [:head
    [:meta {:charset "utf8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome1"}]
    [:meta {:name "description" :content "Palermo web interface"}]
    [:meta {:name "viewport" :content ""}]
    [:title "Palermo"]
    (h/include-css "/stylesheets/application.css"
                   "/stylesheets/bootstrap.css"
                   "/stylesheets/fontawesome.css"
                   "/stylesheets/bootstrap_and_overrides.css")
    (h/include-js  "/javascripts/jquery.js"
                   "/javascripts/jquery_ujs.js"
                   "/javascripts/bootstrap-transition.js"
                   "/javascripts/bootstrap-alert.js"
                   "/javascripts/bootstrap-modal.js"
                   "/javascripts/bootstrap-dropdown.js"
                   "/javascripts/bootstrap-scrollspy.js"
                   "/javascripts/bootstrap-tab.js"
                   "/javascripts/bootstrap-tooltip.js"
                   "/javascripts/bootstrap-popover.js"
                   "/javascripts/bootstrap-button.js"
                   "/javascripts/bootstrap-collapse.js"
                   "/javascripts/bootstrap-carousel.js"
                   "/javascripts/bootstrap-typeahead.js"
                   "/javascripts/bootstrap-affix.js"
                   "/javascripts/bootstrap.js"
                   "/javascripts/bootstrap-resque.js"
                   "/javascripts/failure.js"
                   "/javascripts/jquery.relative-date.js"
                   "/javascripts/polling.js"
                   "/javascripts/relative_date.js"
                   "/javascripts/application.js")]

   [:body 
    [:div {:class "navbar navbar-inverse navbar-fixed-top"}
     [:div {:class "navbar-inner"}
      [:div {:class "container"}
       [:a {:class "btn btn-navbar navbar-toggle"
            :data-toggle "collapse"
            :data-target ".nav-collapse"}
        [:span {:class "icon-bar"}] 
        [:span {:class "icon-bar"}] 
        [:span {:class "icon-bar"}] 
        ]
       [:a {:href "/" :class "brand navbar-brand"} "Palermo"]
       [:div {:class "nav-collapse navbar-collapse collapse"}
        [:ul {:class "nav navbar-nav"}
         [:li [:a {:href "/"} "Overview"]]
         ;[:li [:a {:href "#"} "Working"]]
         [:li [:a {:href "/failures"} "Failures"]]
         [:li [:a {:href "/queues"} "Queues"]]
         [:li [:a {:href "/workers"} "Workers"]]
         ;[:li [:a {:href "#"} "Stats"]]
         ]]]]]

    body

    [:footer {:id "footer"}
     [:div {:class "container"}
      [:p 
       "Powered by "
       [:a {:href "http://github.com/antoniogarrote/palermo"} "Palermo"]
       " v0.2.0 (a job processing system built with &#10084;)"]
      [:p
       "Connected to "
       [:img {:src "images/rabbit.png" :style "width: 15px; margin-top: -8px"}]
       [:a {:href (str "http://" host ":1" port "/#/exchange/%2F/" exchange)}
        (str host ":" port "/" exchange)]]
      ]]]))

;; index page

(defn queues [palermo]
  [:span [:h1 "Queues"]
   [:p {:class "intro"}
    "The list below contains all the registered queues with the number of jobs currently in the queue. Select a queue from above to view all jobs currently pending on the queue."]
   [:table {:class "table table-bordered queues"}
    [:tr 
     [:th "Name"]
     [:th "Jobs"]]
    (let [queues-info (.getQueuesInfo palermo)]
      (map (fn [queue-name]
             (if (not= queue-name "failed")
               [:tr
                [:td {:class "queue"}
                 [:a {:href (str "/queue/" queue-name)}
                  queue-name]]
                [:td {:class "size"}
                 (.get (.get queues-info queue-name) "jobs")]]))
           (.keySet queues-info)))    
     (let [queue-info (.get (.getQueuesInfo palermo) "failed")]
       [:tr {:class "failed first_failure"}
        [:td {:class "queue failed"}
         [:a {:href "/failures"} "failed"]]
        [:td {:class "size"}
         (if (nil? queue-info) 0 (.get queue-info "jobs"))]])]])

(defn working [palermo]
  (let [workers-queues (.getWorkersInfo palermo)
        num-workers (reduce (fn [ac queue-name] 
                              (+ ac (.size (.get workers-queues queue-name))))
                            0 (.keySet workers-queues))
        workers (apply concat (.values workers-queues))]
    [:span [:h1 "Workers"]
     [:p {:class "intro"}
      "The list below contains all workers which are currently running a job."]
     [:table {:class "table table-bordered workers"}
      [:tr
       [:th "Where"]
       [:th "Queue"]
       [:th "Tag"]]
      (if (= num-workers 0)
        [:tr
         [:td {:colspan "4" :class "no-data"}
          "Nothing is happening right now..."]]
        (map (fn [worker-info]
               (let [worker-channel (.get worker-info "channel_details")
                     host (.get worker-channel "peer_host")
                     tag (.get worker-info "consumer_tag")
                     queue-name (.get (.get worker-info "queue") "name")]
                 [:tr
                  [:td {:class "where"} host]
                  [:td {:class "queues queue"} queue-name]
                  [:td {:class "process"}
                   [:code tag]]]))
             workers))]]))

(defn index [palermo]
  (let [host (.get (.show palermo) "host")
        port (.get (.show palermo) "port")
        exchange (.get (.show palermo) "exchange")]
    (layout host port exchange
            [:div {:class "container" :id "main"}
             (queues palermo)
             [:hr]
             (working palermo)])))


;; queue page

(defn queue [palermo queue-name]
  (let [host (.get (.show palermo) "host")
        port (.get (.show palermo) "port")
        exchange (.get (.show palermo) "exchange")
        jobs (.getQueueJobs palermo queue-name)]
    (layout host port exchange
            [:div {:class "container" :id "main"}
             [:h1 "Pending jobs on "
              [:span {:class "hl"} queue-name]]
             [:p {:class "sub"}
              "Showing "
              [:b (.size jobs)]
              " jobs"]
             [:table {:class "table table-bordered jobs"}
              [:tr
               [:th "Message ID"]
               [:th "Class"]
               [:th "Serialization"]
               [:th "Created at"]]
              (map (fn [job]
                     (let [metadata (.get job "metadata")
                           headers (.get metadata "headers")
                           message-id (.get metadata "message-id")
                           serialization (.get metadata "content-type")
                           job-class (.toString (.get headers "job-class"))
                           created-at (java.util.Date. (* 1000 (.get headers "created-at")))
                           sdf (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss z")
                           created-at (.format sdf created-at)]
                       [:tr
                        [:td message-id]
                        [:td job-class]
                        [:td serialization]
                        [:td created-at]]))
                   jobs)
              (when (= (.size jobs) 0)
                [:tr
                 [:td {:class "no-data" :colspan 2}
                  "There are no pending jobs in this queue"]])
              ]
             ])))

;; failed page

(defn failures [palermo]
  (let [host (.get (.show palermo) "host")
        port (.get (.show palermo) "port")
        exchange (.get (.show palermo) "exchange")
        jobs (.getQueueJobs palermo pworker/FAILED_QUEUE)]
    (layout host port exchange
            [:div {:class "container" :id "main"}
             [:h1 "Failed Jobs"]
             (when (> (.size jobs) 0)
               [:span
                (f/form-to [:delete (str "/queues/" pworker/FAILED_QUEUE "/purge")]
                           (f/submit-button {:class "btn btn-danger"
                                             :data {:confirm "Are you sure you want to clear ALL jobs?"}}
                                            (str "Clear " (.size jobs) " jobs")))
                (f/form-to [:put (str "/failures/retry_all")]
                           (f/submit-button {:class "btn"
                                             :data {:confirm "Are you sure you want to retry ALL jobs?"}}
                                            (str "Retry " (.size jobs) " jobs")))])
             [:p {:class "sub"}
              "Showing "
              [:b (.size jobs)]
              " jobs"]
             [:ul {:class "failed"}
              (map (fn [job]
                     (let [
                           metadata (.get job "metadata")
                           headers (.get metadata "headers")
                           message-id (.get metadata "message-id")
                           serialization (.get metadata "content-type")
                           job-class (.toString (.get headers "job-class"))
                           exception (.toString (.get headers "exception-message"))
                           backtrace (.toString (.get headers "stack-trace"))
                           queue (.toString (.get headers "queue"))
                           failed-at (java.util.Date. (* 1000 (.get headers "created-at")))
                           created-at (java.util.Date. (* 1000 (.get headers "created-at")))
                           sdf (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss z")
                           created-at (.format sdf created-at)
                           failed-at (.format sdf failed-at)
                           retries (.get headers "retries")]
                       [:li
                        [:dl
                         [:div {:style "float:right"}
                          [:a {:href (str "/retry/" message-id)} "Retry"]]
                         [:dt "Id"]
                         [:dd message-id]
                         [:dt "Queue"]
                         [:dd queue]
                         [:dt "Class"]
                         [:dd [:code job-class]]
                         [:dt "Serialization"]
                         [:dd serialization]
                         [:dt "Created at"]
                         [:dd created-at]
                         [:dt "Failed at"]
                         [:dd failed-at]
                         [:dt "Retries"]
                         [:dd retries]
                         [:dt "Exception"]
                         [:dd [:code exception]]
                         [:dt "Error"]
                         [:dd {:class "Error"}
                          [:a {:href "#" :class "backtrace"}
                           (if (> (.length backtrace) 300)
                             (.substring backtrace 0 300)
                             backtrace)]
                          [:pre {:style "display:none"} backtrace]]
                         ]
                        [:div {:class "r"}]]))
                   jobs)]])))

;; queues page

(defn all-queues [palermo]
  (let [host (.get (.show palermo) "host")
        port (.get (.show palermo) "port")
        exchange (.get (.show palermo) "exchange")]
    (layout host port exchange
            [:div {:class "container" :id "main"}
             (queues palermo)
             [:hr]])))


;; workers page

(defn all-workers [palermo]
  (let [host (.get (.show palermo) "host")
        port (.get (.show palermo) "port")
        exchange (.get (.show palermo) "exchange")]
    (layout host port exchange
            [:div {:class "container" :id "main"}
             (working palermo)
             [:hr]])))
