;;;; IMDb List Analyzer main program
;;;
;;; Analyze IMDb CSV files that contain movie ratings data.
;;; The analysis covers the following statistics:
;;; * Number: number of rated movie titles OR number of rated titles both lists contain
;;; * Mean: arithmetic mean of your ratings
;;; * Standard deviation: standard deviation of your movie ratings
;;; * Correlation: correlation coefficient between ratings and IMDb averages (or other ratings)
;;; * Entropy: information content of one rating, measured in bits, based on Shannon entropy
;;; * Best directors: directors whose movies you rate highly (custom statistical test)
;;; * Worst directors: directors whose movies you rate poorly (custom statistical test)
;;;
;;; Anyone with an IMDb account can retrieve their own ratings file as follows.
;;; 1. Login to www.imdb.com with you account.
;;; 2. Search for a personal "Your Ratings" view that contains all your rated movies.
;;; 3. Click "Export this list" at the bottom of the page.
;;; 4. Save file into the filesystem.
;;; 5. Launch this program with a command-line argument that is the filepath of downloaded CSV file.
;;;
;;; Esa Junttila 2015-11-01 (originally 2013-06-29)

(ns imdb-list-analyzer.clj.core
  (:require [imdb-list-analyzer.clj.imdb-data :as imdb]
            [imdb-list-analyzer.clj.result-view :as resview]
            [imdb-list-analyzer.clj.dual-result-view :as dualview]
            [imdb-list-analyzer.clj.server :refer [start-server]])
  (:import (java.io File))
  (:gen-class))

(defn missing-file-err
  "Report into stderr about a missing filename."
  [filename]
  (binding [*out* *err*]  ; writing to stderr instead of stdout
    (println (str "Cannot find input file: " filename))))

(defn file-exists?
  "Does a given filename exist in the filesystem?"
  [filename]
  (.exists ^File (clojure.java.io/as-file filename)))

(defn one-input-analysis
  "Analyze a single IMDb ratings list, given as a CSV sequence
  of string sequences, with a header. Return an AnalysisResult record."
  [input-data]
  (resview/compute-results (rest (imdb/parse-imdb-data input-data))))

(defn one-json-input-analysis
  "Analyze a single IMDb ratings list, given as a JSON string.
  Return a JSON string of an AnalysisResult record."
  [json-str]
  (let [titles-coll (imdb/parse-imdb-data-from-json-str json-str)]
    (resview/jsonify-single-result (resview/compute-results (rest titles-coll)))))

(defn one-file-analysis
  "Analyze a single IMDb ratings list, in CSV format, given as a filename
  or a file. Write the results into stdout. If there is no file
  matching with given file, report to stderr."
  [file]
  (if (file-exists? file)
    (one-input-analysis (imdb/read-raw-data file))
    (missing-file-err file)))

(defn dual-input-analysis
  "Analyze two IMDb rating lists. Each input arg refers to
  a collection of IMDb ratings list data: a sequence of string sequences (CSV).
  Return a DualAnalysisResult record."
  [input-data-a input-data-b]
  (dualview/compute-dual-results
    (rest (imdb/parse-imdb-data input-data-a))
    (rest (imdb/parse-imdb-data input-data-b))))

(defn dual-json-input-analysis
  "Analyze two IMDb ratings lists, given as JSON strings.
  Return a JSON string of a DualAnalysisResult record."
  [json-str-a json-str-b]
  (dualview/jsonify-dual-result
    (dualview/compute-dual-results
      (rest (imdb/parse-imdb-data-from-json-str json-str-a))
      (rest (imdb/parse-imdb-data-from-json-str json-str-b)))))

(defn dual-file-analysis
  "Analyze two IMDb rating lists. Each input refers to a file or filename
  of a CSV-formatted IMDb ratings list. Return an AnalysisResult record."
  [file-a, file-b]
  (cond
    (not (file-exists? file-a)) (missing-file-err file-a)
    (not (file-exists? file-b)) (missing-file-err file-b)
    :else (dual-input-analysis
            (imdb/read-raw-data file-a)
            (imdb/read-raw-data file-b))))

(defn print-usage []
  (println
    "  Analyze an IMDb CSV file (movie ratings data), or two files.\n"
    "  Usage:\n"
    "   lein run <filenameA> [filenameB]\n"
    "  Examples:\n"
    "   lein run resources\\example_ratings_A.csv\n"
    "   lein run resources\\example_ratings_A.csv resources\\example_ratings_B.csv"))

(defn -main
  "Run IMDb analyzer on given IMDb ratings file or files.
  Show analysis results on-screen. Read usage and documentation."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  ;TODO Temporary proof-of-concept for testing, fix with proper command line options
  (let [port (-> args first Integer.)]
    (start-server port))
  ;temporary disable
  #_(case (count args)
      0 (print-usage)
      1 (when-let [res (one-file-analysis (first args))]
          (do
            (resview/view-results res)
            (print (one-json-input-analysis (imdb/convert-csv-to-json-str (imdb/read-raw-data (first args)))))))
      2 (when-let [res (dual-file-analysis (first args) (second args))]
          (do
            (dualview/view-dual-results res)
            (print (dual-json-input-analysis
                     (imdb/convert-csv-to-json-str (imdb/read-raw-data (first args)))
                     (imdb/convert-csv-to-json-str (imdb/read-raw-data (second args)))))))
      (print-usage)))
