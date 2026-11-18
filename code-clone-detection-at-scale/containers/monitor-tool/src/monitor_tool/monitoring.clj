(ns monitor-tool.monitoring
  (:require [clojure.string :as string]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]
            [monitor-tool.db-monitor :as db-monitor]))

(def monitoring-data (atom {:counts {}
                            :entries []}))
(def monitoring-server (atom nil))

;; Log with timestamp
(defn ts-println
  "Log a timestamped message."
  [& args]
  (let [message (apply str args)
        timestamp (.toString (java.time.LocalDateTime/now))]
    (println timestamp message)))

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
  (ts-println "[INFO] Starting Monitoring Server on port 9090...")
  (println)
  (reset! monitoring-server
          (jetty/run-jetty (wrap-json-response
                            (fn [req]
                              (case (:uri req)
                                "/monitoring" (monitoring-endpoint req)
                                "/health" (health-endpoint req)
                                (not-found-handler req))))
                           {:port 9090 :join? false})))

;; Database Polling Integration
(defn start-database-monitoring []
  "Start monitoring the database and updating monitoring data."
  (db-monitor/monitor-database
    (fn [data]
      (swap! monitoring-data update :counts merge (:counts data))
      (ts-println "[INFO] Monitoring Status: " (:counts data)))))

;; Application Entry Point
(defn -main [& args]
  "Start the monitor-tool application."
  (ts-println "[INFO] Starting MonitorTool...")
  (println)
  (start-monitoring-server)
  (start-database-monitoring))
