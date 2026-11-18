(ns cljdetector.monitoring
  (:require [clojure.string :as string]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]))

(def monitoring-data (atom []))
(def monitoring-server (atom nil))

;; Log with timestamp
(defn ts-println
  "Log a timestamped message and optionally store it via a callback."
  [& args]
  (let [[maybe-callback & rest-args] args
        log-callback (if (fn? maybe-callback) maybe-callback nil)
        message (if log-callback
                  (apply str rest-args)
                  (apply str args))
        timestamp (.toString (java.time.LocalDateTime/now))]
    (println timestamp message)
    (when log-callback
      (log-callback timestamp message))))

;; Monitor Timing
(defn monitor-timing [label f & args]
  "Measure and log the execution time of a function, including start and end timestamps."
  (if (fn? f)
    (let [start-time (System/nanoTime)
          start-ts (.toString (java.time.LocalDateTime/now))
          result (apply f args)
          end-time (System/nanoTime)
          end-ts (.toString (java.time.LocalDateTime/now))
          duration-ms (/ (- end-time start-time) 1e6)]
      (ts-println "[INFO] " label " started at " start-ts)
      (ts-println "[INFO] " label " ended at " end-ts)
      (ts-println "[INFO] " label " took " duration-ms " ms.")
      result)
    (do
      (ts-println "[ERROR] " label " received a non-function value: " f)
      nil)))

;; Log Memory Usage
(defn log-memory-usage [log-callback]
  "Log the current memory usage of the JVM with timestamp."
  (let [runtime (Runtime/getRuntime)
        used-memory (/ (- (.totalMemory runtime) (.freeMemory runtime)) 1048576.0)
        timestamp (.toString (java.time.LocalDateTime/now))]
    (ts-println log-callback (str "[INFO] Memory Used: " used-memory " MB"))))

;; Monitoring Data
(defn add-monitoring-data [label value]
  "Add a custom monitoring data entry with a timestamp."
  (swap! monitoring-data conj {:label label :value value :timestamp (str (java.time.LocalDateTime/now))}))

;; Final Statistics
(defn add-final-statistics [statistics]
  "Add final statistics to the monitoring data."
  (add-monitoring-data "Final Statistics" statistics))

;; Monitoring Endpoint
(defn monitoring-endpoint [req]
  "Endpoint to retrieve current monitoring data."
  (response @monitoring-data))

;; Health Endpoint
(defn health-endpoint [req]
  "Endpoint to check the health of the monitoring system."
  (response {:status "UP"
             :timestamp (str (java.time.LocalDateTime/now))
             :memory-used (str (/ (- (.totalMemory (Runtime/getRuntime))
                                     (.freeMemory (Runtime/getRuntime)))
                             1048576.0) " MB")}))

(defn not-found-handler [req]
  "Fallback handler for unknown routes."
  {:status 404
   :headers {"Content-Type" "application/json"}
   :body {:error "Endpoint not found"}})

;; Monitoring Server
(defn start-monitoring-server []
  "Start the server for monitoring endpoints."
  (ts-println "[INFO] Starting Monitoring Server on port 8081...")
  (println)
  (reset! monitoring-server
          (jetty/run-jetty (wrap-json-response
                            (fn [req]
                              (case (:uri req)
                                "/monitoring" (monitoring-endpoint req)
                                "/health" (health-endpoint req)
                                (not-found-handler req))))
                           {:port 8081 :join? false})))
