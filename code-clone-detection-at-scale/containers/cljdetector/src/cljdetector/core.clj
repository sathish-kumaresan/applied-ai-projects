(ns cljdetector.core
  (:require [clojure.string :as string]
            [cljdetector.process.source-processor :as source-processor]
            [cljdetector.process.expander :as expander]
            [cljdetector.storage.storage :as storage]
            [cljdetector.monitoring :as monitoring]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]))

;; Global Configurations
(def DEFAULT-CHUNKSIZE 5)
(def source-dir (or (System/getenv "SOURCEDIR") "/tmp"))
(def source-type #".*\.java")

;; Database Operations
(defn maybe-clear-db []
  "Cleanup the database"
  (monitoring/ts-println storage/addUpdate! (str "[INFO] Database Cleanup..."))
  (storage/clear-db!))
  (println)

;; File Processing
(defn maybe-read-files [args]
  "Read and process files."
  (when-not (some #{"NOREAD"} (map string/upper-case args))
    (monitoring/monitor-timing "Processing Files and Chunks"
      (fn []
        (monitoring/ts-println storage/addUpdate! (str "[INFO] Processing Files and Chunks..."))
        (println)
        (let [chunk-param (System/getenv "CHUNKSIZE")
              chunk-size (if chunk-param (Integer/parseInt chunk-param) DEFAULT-CHUNKSIZE)
              file-handles (source-processor/traverse-directory source-dir source-type)
              chunks (source-processor/chunkify chunk-size file-handles)
              total-files (count file-handles)
              total-chunks (count (flatten chunks))]
          
          ;; Store files
          (storage/store-files! file-handles)
          (monitoring/add-monitoring-data "Processed Files" (str "Files: " total-files))
          (monitoring/log-memory-usage storage/addUpdate!)
          (println)

          ;; Store chunks
          (storage/store-chunks! chunks)
          (monitoring/add-monitoring-data "Processed Chunks" (str "Chunks: " total-chunks))
          (monitoring/log-memory-usage storage/addUpdate!)
          (println))))
    ;(monitoring/log-memory-usage storage/addUpdate!)                                                                                                  
    (println)))

;; Clone Detection
(defn maybe-detect-clones [args]
  "Detect clones"
  (when-not (some #{"NOCLONEID"} (map string/upper-case args))
    (monitoring/monitor-timing "Detecting Clones"
      (fn []
        (monitoring/ts-println storage/addUpdate! (str "[INFO] Detecting Clones..."))
        (println)

        ;; Identify candidates
        (monitoring/ts-println (str "[INFO] Identifying Clone Candidates..."))
        (storage/identify-candidates!)
        (let [candidates (storage/count-items "candidates")]
          (monitoring/ts-println storage/addUpdate! (str "[INFO] Identified " candidates " Clone Candidates."))
          (monitoring/add-monitoring-data "Identified Candidates" (str "Candidates: " candidates)))
        (monitoring/log-memory-usage storage/addUpdate!)

        ;; Expand clones
        (expander/expand-clones)
        (monitoring/log-memory-usage storage/addUpdate!)
        (println)))
    ;(monitoring/log-memory-usage storage/addUpdate!)
    (println)))

;; Clone Listing
(defn pretty-print [clones]
  "Print the detected clones."
  (doseq [clone clones]
    (println "====================\n" "Clone with" (count (:instances clone)) "instances:")
    (doseq [inst (:instances clone)]
      (println "  -" (:fileName inst) "startLine:" (:startLine inst) "endLine:" (:endLine inst)))
    (println "\nContents:\n----------\n" (:contents clone) "\n----------")))

(defn maybe-list-clones [args]
  "List clones."
  (when (some #{"LIST"} (map string/upper-case args))
    (monitoring/ts-println storage/addUpdate! (str "[INFO] Consolidating and listing clones..."))
    (pretty-print (storage/consolidate-clones-and-source))
    (monitoring/add-monitoring-data "Clone Listing" "Completed")))

;; Final Statistics
(defn add-final-statistics []
  "Add the final statistics to the monitoring data."
  (monitoring/add-final-statistics
    (str "Files: " (storage/count-items "files")
         ", Chunks: " (storage/count-items "chunks")
         ", Candidates: " (storage/count-items "candidates")
         ", Clones: " (storage/count-items "clones"))))

;; Starting Point for All-At-Once Clone Detection
(defn -main [& args]
  "Starting Point for All-At-Once Clone Detection
  Arguments:
   - Clear clears the database
   - NoRead do not read the files again
   - NoCloneID do not detect clones
   - List print a list of all clones"

  ;; Clear Database at Start
  (maybe-clear-db)

  ;; Ensure Indexes are created in the Database
  (println)
  (storage/create-index)

  ;; Start Monitoring Server
  (println)
  (monitoring/ts-println storage/addUpdate! (str "[INFO] Application Starting..."))
  (println)
  (monitoring/start-monitoring-server)

  (monitoring/add-monitoring-data "Start Time" (.toString (java.time.LocalDateTime/now)))

  (monitoring/monitor-timing "Application Execution"
    (fn []
      ;; Process Files
      (maybe-read-files args)
      (maybe-detect-clones args)
      (maybe-list-clones args)

      ;; Final Summary & Statistics
      (monitoring/ts-println storage/addUpdate! (str "[INFO] Application Finished"))
      (println)
      (monitoring/ts-println "======== Final Summary & Statistics ========")
      (storage/print-statistics)
      (add-final-statistics)
      (println)))

  (monitoring/add-monitoring-data "End Time" (.toString (java.time.LocalDateTime/now)))

  ;; Keep Monitoring Server Alive
  (println)
  (monitoring/ts-println (str "[INFO] Monitoring server is still running..."))
  (monitoring/ts-println (str "[INFO] Health: http://localhost:8081/health"))
  (monitoring/ts-println (str "[INFO] Monitoring: http://localhost:8081/monitoring"))
  (monitoring/ts-println (str "[INFO] Press Ctrl+C to stop.")))
