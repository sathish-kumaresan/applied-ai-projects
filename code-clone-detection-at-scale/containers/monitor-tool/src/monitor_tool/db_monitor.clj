(ns monitor-tool.db-monitor
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]))

;; Database Configuration
(def hostname (or (System/getenv "DBHOST") "localhost"))
(def dbname (or (System/getenv "DBNAME") "cloneDetector"))
(def collnames ["files" "chunks" "candidates" "clones" "statusUpdates"])
(defonce db-connection (atom nil))

;; Log
(defn ts-println
  "Log a timestamped message."
  [& args]
  (let [message (apply str args)
        timestamp (.toString (java.time.LocalDateTime/now))]
    (println timestamp message)))

;; Database Utilities
(defn get-db-connection []
  "Get or initialize a shared MongoDB connection."
  (if @db-connection
    @db-connection
    (do
      (ts-println "[INFO] Initializing shared MongoDB connection...")
      (reset! db-connection (mg/connect {:host hostname})))))

(defn get-db []
  "Get the database object."
  (let [conn (get-db-connection)]
    (mg/get-db conn dbname)))

;; Fetch Collection Counts
(defn fetch-collection-counts []
  "Fetch the counts of documents in each monitored collection."
  (let [db (get-db)]
    (ts-println "[INFO] Fetching collection counts...")
    (into {} (map (fn [coll] [coll (mc/count db coll)]) collnames))))

;; Monitor Database
(defn monitor-database [callback]
  "Monitor database collections."
  (ts-println "[INFO] Starting database monitoring...")
  (future
    (loop []
      (try
        (let [counts (fetch-collection-counts)]
          (callback {:counts counts}))
        (catch Exception e
          (ts-println "[ERROR] Error during database monitoring: " (.getMessage e))))
      (Thread/sleep 20000)
      (recur))))
