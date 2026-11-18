(ns cljdetector.process.source-processor
  (:use [clojure.java.io])
  (:require [clojure.string :as string]
            [clj-commons.digest :as digest]))

(def emptyLine (re-pattern "^\\s*$"))
(def oneLineComment (re-pattern "//.*"))
(def oneLineMultiLineComment (re-pattern "/\\*.*?\\*/"))
(def openMultiLineComment (re-pattern "/\\*+[^*/]*$"))
(def closeMultiLineComment (re-pattern "^[^*/]*\\*+/"))

(defn process-lines [lines]
  (loop [result [{:lineNumber 0 :contents "" :lineType "startLine"}]
         remaining-lines lines]
    (if (empty? remaining-lines)
      (rest result) ;; Drop the initial startLine
      (let [prev-line (last result)
            item (first remaining-lines)
            index (inc (:lineNumber prev-line))
            new-line (cond
                       (and (= (:lineType prev-line) "multiLineComment")
                            (re-matches closeMultiLineComment item))
                       {:lineNumber index :contents (string/trim (string/replace item closeMultiLineComment "")) :lineType "lastMultiLineComment"}

                       (= (:lineType prev-line) "multiLineComment")
                       {:lineNumber index :contents "" :lineType "multiLineComment"}

                       (re-matches emptyLine item)
                       {:lineNumber index :contents "" :lineType "emptyLine"}

                       (re-matches oneLineComment item)
                       {:lineNumber index :contents (string/trim (string/replace item oneLineComment "")) :lineType "oneLineComment"}

                       (re-matches oneLineMultiLineComment item)
                       {:lineNumber index :contents (string/trim (string/replace item oneLineMultiLineComment "")) :lineType "oneLineMultiLineComment"}

                       (re-matches openMultiLineComment item)
                       {:lineNumber index :contents (string/trim (string/replace item openMultiLineComment "")) :lineType "multiLineComment"}

                       :else
                       {:lineNumber index :contents (string/trim item) :lineType "normal"})]
        (recur (conj result new-line) (rest remaining-lines))))))

(defn chunkify-file [chunkSize file]
  (try
    (let [fileName (.getPath file)
          filteredLines (vec (filter #(not (= "" (:contents %)))
                                     (-> file
                                         slurp
                                         (string/split #"\n")
                                         process-lines)))
          totalLines (count filteredLines)]
      (map (fn [idx]
             (let [chunk (subvec filteredLines idx (+ idx chunkSize))
                   startLine (:lineNumber (first chunk))
                   endLine (:lineNumber (last chunk))
                   hash (digest/md5 (string/join "\n" (map :contents chunk)))]
               {:fileName fileName :startLine startLine :endLine endLine :chunkHash hash}))
           (range 0 (- totalLines chunkSize))))
    (catch Exception e [])))

(defn chunkify [chunkSize files]
  (pmap #(chunkify-file chunkSize %) files))

(defn traverse-directory [path pattern]
  (filter #(re-matches pattern (.getName %))
          (tree-seq #(.isDirectory %)
                    #(.listFiles %)
                    (file path))))
