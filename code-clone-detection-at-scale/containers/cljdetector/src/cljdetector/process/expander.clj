(ns cljdetector.process.expander
  (:require [clojure.string :as string]
            [cljdetector.storage.storage :as storage]
            [cljdetector.monitoring :as monitoring]))

;; Common to both solutions
;; ----------------------------------------

(defn sort-instances [instances]
  "Sort clone instances by file name and start line."
  (sort-by (juxt :fileName :startLine) instances))

(defn merge-clones [cand-a cand-b]
  "Merge overlapping clone instances."
  (let [inst-a (sort-instances (:instances cand-a))
        inst-b (sort-instances (:instances cand-b))]
    {:_id (:_id cand-a)
     :numberOfInstances (:numberOfInstances cand-a)
     :instances (map (fn [ia ib]
                       {:fileName (:fileName ia)
                        :startLine (min (:startLine ia) (:startLine ib))
                        :endLine (max (:endLine ia) (:endLine ib))})
                     inst-a inst-b)}))

;; FP Solution
;; ----------------------------------------

(defn overlap-instance? [cand-a cand-b]
  "Check if two instances overlap in the same file."
  (and (= (:fileName cand-a) (:fileName cand-b))
       (or (and (< (:startLine cand-a) (:startLine cand-b))
                (>= (:endLine cand-a) (:startLine cand-b)))
           (and (< (:startLine cand-b) (:startLine cand-a))
                (>= (:endLine cand-b) (:startLine cand-a))))))

(defn overlaps? [cand-a cand-b]
  "Check if two candidates overlap based on their instances."
  (let [inst-a (sort-instances (:instances cand-a))
        inst-b (sort-instances (:instances cand-b))]
    (every? true? (map overlap-instance? inst-a inst-b))))

(defn expand-clones-elegantly [candidates]
  "Expand clones using FP solution."
  (reduce (fn [clones candidate]
            (let [{overlapping true remaining false} (group-by #(overlaps? candidate %) clones)]
              (if (empty? overlapping)
                  (conj clones candidate)
                  (conj remaining (reduce merge-clones candidate overlapping)))
            )) [] candidates))

;; Aleph-null BoBoTW solution
;; ----------------------------------------

(defn maybe-expand [dbconnection candidate]
  "Expand clones using Aleph-null BoBoTW solution."
    (loop [overlapping (storage/get-overlapping-candidates dbconnection candidate)
           clone candidate]
      (if (empty? overlapping)
        (do
          (storage/remove-overlapping-candidates! dbconnection (conj overlapping candidate))
          clone)
        (let [merged-clone (reduce merge-clones clone overlapping)]
          (storage/remove-overlapping-candidates! dbconnection overlapping)
          (recur (storage/get-overlapping-candidates dbconnection merged-clone)
                 merged-clone)))))

(defn expand-clones []
  "Expand clone candidates and storing clones in the database."
  (monitoring/monitor-timing "Expanding Clone Candidates and Storing Clones"
    (fn []
      (println)
      (monitoring/ts-println storage/addUpdate! (str "[INFO] Expanding Clone Candidates and Storing Clones..."))
      (let [dbconnection (storage/get-dbconnection)
            total-candidates (storage/count-items "candidates")
            total-clone-size (atom 0)]
        (loop [candidate (storage/get-one-candidate dbconnection)]
          (if candidate
            (let [result (maybe-expand dbconnection candidate)
                  clone-size (storage/store-clones! [result])]
              (do
                (swap! total-clone-size + clone-size)
                ; Log progress
                (let [remaining-candidates (storage/count-items "candidates")
                      current-clones (storage/count-items "clones")
                      processed-candidates (- total-candidates remaining-candidates)]
                  (monitoring/ts-println
                    (str "[INFO] Expansion Status: Clones Identified --> " current-clones
                         " | Candidates --> Total: " total-candidates
                         "; Processed: " processed-candidates
                         "; Remaining: " remaining-candidates)))
                (recur (storage/get-one-candidate dbconnection))))
            (let [total-clones (storage/count-items "clones")]
              (do
                ; Final summary
                (println)
                (monitoring/ts-println "======== Expansion Summary ========")
                (monitoring/ts-println storage/addUpdate! (str "[INFO] Candidates Processed: " total-candidates))
                (monitoring/ts-println storage/addUpdate! (str "[INFO] Clones Expanded: " total-clones))
                (monitoring/ts-println storage/addUpdate! (str "[INFO] Total Clone Size (lines): " @total-clone-size))
                (monitoring/ts-println storage/addUpdate! (str "[INFO] Average Clone Size (lines): " (if (pos? total-clones) (double (/ @total-clone-size total-clones)) 0)))
                (println)

                ; Add monitoring data
                (monitoring/add-monitoring-data "Expansion Summary" (str "Expanded Clones: " total-clones))))))))))
