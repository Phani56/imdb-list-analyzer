(ns imdb-list-analyzer.cljs.utils
  (:require
    [goog.string :as gstring]
    [goog.string.format]))

(defn round-num [num precision]
  (if (nil? num)
    num
    (gstring/format (str "%." precision "f") num)))

(defn map-keywords-to-int [m]
  "Converts map's keys from keyword to int."
  (reduce #(assoc %1 (-> %2 key name int) (val %2)) {} m))

(defn rgb-str [red green blue]
  "Function generates CSS-formatted rgb-string
  that can be used e.g. with NVD3 library."
  (str "rgb(" red "," green "," blue ")"))
