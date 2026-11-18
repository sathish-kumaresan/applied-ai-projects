(ns cljdetector.storage.storage
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.conversion :refer [from-db-object]]
            [cljdetector.monitoring :as monitoring]
            [clojure.string :as string]))

;; Database Configuration
(def hostname (or (System/getenv "DBHOST") "localhost"))
(def dbname (or (System/getenv "DBNAME") "cloneDetector"))
(def partition-size 100)
(def collnames (or (System/getenv "COLLNAMES") ["files" "chunks" "candidates" "clones" "statusUpdates"]))
(defonce db-connection (atom nil))

;; Database Utilities
(defn get-dbconnection []
  "Get or initialize a shared MongoDB connection."
  (if @db-connection
    @db-connection
    (reset! db-connection (mg/connect {:host hostname}))))

(defn get-db []
  "Get the database object."
  (let [conn (get-dbconnection)]
    (mg/get-db conn dbname)))

;; Add Status Update
(defn addUpdate! [timestamp message]
  "Add status updates to the monitoring database."
  (let [db (get-db)]
    (mc/insert db "statusUpdates" {:timestamp timestamp :message message})))

;; Database Cleanup
(defn clear-db! []
  "Drop all collections in the database."
  (monitoring/monitor-timing "Clearing Database"
    (fn []
      (let [db (get-db)]
        (doseq [coll collnames]
          (mc/drop db coll))
        (monitoring/ts-println addUpdate! (str "[INFO] Cleared Database."))))))

;; Create Indexes
(defn create-index []
  (let [db (get-db)]
    (mc/create-index db "candidates" {:instances.fileName 1 :instances.startLine 1 :instances.endLine 1})
    (monitoring/ts-println addUpdate! (str "[INFO] Created Database Index."))))

;; Print Statistics
(defn print-statistics []
  "Print the count of documents in each collection."
  (let [db (get-db)]
    (doseq [coll collnames]
      (monitoring/ts-println (str "[INFO] DB contains " (mc/count db coll) " " coll)))))

;; Count Items
(defn count-items [collname]
  "Count the number of items in a specific collection."
  (let [db (get-db)]
    (mc/count db collname)))

;; File Storage
(defn store-files! [files]
  "Store file data in the database."
  (monitoring/monitor-timing "Storing Files"
    (fn []
      (let [db (get-db)
            collname "files"
            file-parted (partition-all partition-size files)]
        (try
          (monitoring/ts-println (str "[INFO] Storing Files..."))
          (doseq [file-group file-parted]
            (mc/insert-batch db collname (map #(hash-map :fileName (.getPath %) :contents (slurp %)) file-group)))
          (monitoring/ts-println addUpdate! (str "[INFO] " (count files) " files stored successfully."))
          (catch Exception e
            (monitoring/ts-println addUpdate! (str "[ERROR] Error storing files: " (.getMessage e)))))))))

;; Chunk Storage
(defn store-chunks! [chunks]
  "Store file chunks in the database."
  (monitoring/monitor-timing "Storing Chunks"
    (fn []
      (let [db (get-db)
            collname "chunks"
            flat-chunks (flatten chunks)
            chunk-parted (partition-all partition-size flat-chunks)]
        (try
          (monitoring/ts-println (str "[INFO] Storing Chunks..."))
          (doseq [chunk-group chunk-parted]
            (mc/insert-batch db collname chunk-group))
          (monitoring/ts-println addUpdate! (str "[INFO] " (count flat-chunks) " chunks stored successfully."))
          (catch Exception e
            (monitoring/ts-println addUpdate! (str "[ERROR] Error storing chunks: " (.getMessage e)))))))))

;; Clone Storage
(defn store-clones! [clones]
  "Store clones in the database."
  (let [db (get-db)
        collname "clones"
        clones-parted (partition-all partition-size clones)
        total-clone-size (atom 0)]
    (try
      (doseq [clone-group clones-parted]
        (mc/insert-batch db collname clone-group)
        (doseq [clone clone-group]
          (let [clone-size (reduce + (map (fn [inst]
                                             (- (:endLine inst) (:startLine inst) -1))
                                           (:instances clone)))]
            (swap! total-clone-size + clone-size))
          (monitoring/ts-println
            (str "[INFO] Clone Stored in the DB --> Number of Instances: " (:numberOfInstances clone) "\n"
                 "Cloned Files & Lines -->\n"
                 (string/join "\n" (map (fn [inst]
                                          (str "  File: " (:fileName inst)
                                               " | Lines: " (:startLine inst) "-" (:endLine inst)))
                                        (:instances clone)))))))
      @total-clone-size
      (catch Exception e
        (monitoring/ts-println addUpdate!
          (str "[ERROR] Error storing clones in DB: " (.getMessage e)))))))

;; Candidate Identification
(defn identify-candidates! []
  "Identify candidate chunks for clone detection."
  (monitoring/monitor-timing "Identifying Clone Candidates"
    (fn []
      (let [db (get-db)
            collname "chunks"]
        (mc/aggregate db collname
                      [{$group {:_id {:chunkHash "$chunkHash"}
                                :numberOfInstances {$count {}}
                                :instances {$push {:fileName "$fileName"
                                                   :startLine "$startLine"
                                                   :endLine "$endLine"}}}}
                       {$match {:numberOfInstances {$gt 1}}}
                       {"$out" "candidates"}])))))

;; Clone Consolidation
(defn consolidate-clones-and-source []
  "Aggregate clones."
  (let [db (get-db)
        collname "clones"]
    (mc/aggregate db collname
                  [{$project {:_id 0 :instances "$instances" :sourcePosition {$first "$instances"}}}
                   {"$addFields" {:cloneLength {"$subtract" ["$sourcePosition.endLine" "$sourcePosition.startLine"]}}}
                   {$lookup
                    {:from "files"
                     :let {:sourceName "$sourcePosition.fileName"
                           :sourceStart {"$subtract" ["$sourcePosition.startLine" 1]}
                           :sourceLength "$cloneLength"}
                     :pipeline
                     [{$match {$expr {$eq ["$fileName" "$$sourceName"]}}}
                      {$project {:contents {"$split" ["$contents" "\n"]}}}
                      {$project {:contents {"$slice" ["$contents" "$$sourceStart" "$$sourceLength"]}}}
                      {$project {:_id 0
                                 :contents
                                 {"$reduce"
                                  {:input "$contents"
                                   :initialValue ""
                                   :in {"$concat"
                                        ["$$value"
                                         {"$cond" [{"$eq" ["$$value", ""]}, "", "\n"]}
                                         "$$this"]}}}}}]}
                     :as "sourceContents"}
                   {$project {:_id 0 :instances 1 :contents "$sourceContents.contents"}}])))

;; Candidate Expansion
(defn get-one-candidate [conn]
  "Retrieve one candidate from the database."
  (let [db (get-db)
        collname "candidates"]
    (from-db-object (mc/find-one db collname {}) true)))

(defn get-overlapping-candidates [conn candidate]
  (let [db (get-db)
        collname "candidates"
        clj-cand (from-db-object candidate true)]
    (mc/aggregate db collname
                  [{$match {"instances.fileName" {$all (map #(:fileName %) (:instances clj-cand))}}}
                   {$addFields {:candidate candidate}}
                   {$unwind "$instances"}
                   {$project
                    {:matches
                     {$filter
                      {:input "$candidate.instances"
                       :cond {$and [{$eq ["$$this.fileName" "$instances.fileName"]}
                                    {$or [{$and [{$gt  ["$$this.startLine" "$instances.startLine"]}
                                                 {$lte ["$$this.startLine" "$instances.endLine"]}]}
                                          {$and [{$gt  ["$instances.startLine" "$$this.startLine"]}
                                                 {$lte ["$instances.startLine" "$$this.endLine"]}]}]}]}}}
                     :instances 1
                     :numberOfInstances 1
                     :candidate 1
                     }}
                   {$match {$expr {$gt [{$size "$matches"} 0]}}}
                   {$group {:_id "$_id"
                            :candidate {$first "$candidate"}
                            :numberOfInstances {$max "$numberOfInstances"}
                            :instances {$push "$instances"}}}
                   {$match {$expr {$eq [{$size "$candidate.instances"} "$numberOfInstances"]}}}
                   {$project {:_id 1 :numberOfInstances 1 :instances 1}}])))

(defn remove-overlapping-candidates! [conn candidates]
  "Remove overlapping candidates from the database."
  (let [db (get-db)
        collname "candidates"]
    (mc/remove db collname {:_id {$in (map #(:_id %) candidates)}})))

(defn store-clone! [conn clone]
  "Store clone in the database."
    (let [db (get-db)
          collname "clones"
          anonymous-clone (select-keys clone [:numberOfInstances :instances])]
      (try
        (mc/insert db collname anonymous-clone)
        (monitoring/ts-println addUpdate! (str "[INFO] Clone Stored in the DB"))
        (catch Exception e
          (monitoring/ts-println addUpdate!
            (str "[ERROR] Error storing clone in the DB: " (.getMessage e)))))))
