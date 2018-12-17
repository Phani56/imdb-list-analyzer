(ns imdb-list-analyzer.cljs.graphs
  (:require
    [imdb-list-analyzer.cljs.utils :as utils]
    [clojure.set]))

(defn map-to-pair-data [m]
  "Function converts map to appropriate format format for NVD3 histrogram:
  {'foo1' 'bar1', 'foo2' 'bar2} -> [{:x 'foo1' :y 'bar1'} {:x 'foo2' :y 'bar2'}]"
  (reduce #(conj %1 {:x (key %2) :y (val %2)}) [] m))

(defn dir-data-to-scatter [dir-data]
  (reduce #(conj %1
                 {:values [{:x (count (last (first %2)))
                            :y (last %2)}]
                  :key (first (first %2))})
          [] dir-data))

(defn discrepancy-data-to-value-label-map [disc-data]
  (reduce #(conj %1
                 {:value (:discrepancy %2)
                  :label (str (:title %2) " - Rating: " (:rate %2) " IMDB: " (:imdb-rate %2))})
          [] disc-data))

(defn discrepancy-data-to-line-chart [disc-data]
  (reduce #(conj %1
                 {:values [{:x (:discrepancy %2)
                            :y 0}]
                  :key (str (:title %2) " - Rating: " (:rate %2) " IMDB: " (:imdb-rate %2))})
          [] disc-data))

(defn group-duplicates-helper [acc next]
  "Helper function for 'group-dir-scatter-duplicates'.
  In addition to accumulated results function gets a next data point that is
  grouped based on :values. The format of the next data point is:
  'foo' [{:values 'foo' :key 'bar1'} {:values 'foo' :key 'bar2'} .. ]
  Funtion combines all (:key)s to a vector so that the returned map is:
  {:values 'foo' :key ['bar1' 'bar2' ..]}
  This map is added to a given acc-sequence."
  (conj acc {:values (first next)
             :key (reduce #(conj %1 (:key %2)) [] (second next))}))

(defn group-dir-scatter-duplicates
  "Groupes duplicate value pairs from NVD3-formatted data and replaces
   :key to be a sequence of grouped items :key. This grouping is necessary
   as there is a bug in NVD3 that causes a crash if there occurs duplicate
   value pairs in data."
  [dir-scatter-data]
  (reduce group-duplicates-helper [] (group-by :values dir-scatter-data)))

(defn add-color-to-scatter [dir-scatter-data scale-key]
  "Adds new key-value pair :color for each scatter point.
  Color value is determined based key-value (e.g. :x or :y).
  Color is sclaed from red (low key-value) to blue (high key-value)"
  (reduce #(let [score (scale-key (first (:values %2)))]
            (conj %1 (assoc %2 :color (utils/rgb-str
                                        (int (* 255 (- 1 score)))
                                        0
                                        (int (* 255 score))))))
          []
          dir-scatter-data))

(defn make-histogram! [data html-svg-loc-str]
  "NVD3 multiBarChart graph. Data-parmeter should be a map containing :singleresults
  (e.g. core.cljs/@app-state after the csv-analysis). Html-svg-loc-str is the location of the svg-element,
  e.g. '#barchart svg' "
  (when data
    (.addGraph js/nv (fn []
                       (let [chart (.. js/nv -models multiBarChart)]
                         (.. chart -tooltip (enabled false))
                         (.. chart -xAxis
                             (tickFormat (.format js/d3 ",d"))
                             (axisLabel "Rating"))
                         (.. chart -yAxis
                             (tickFormat (.format js/d3 ",f"))
                             (axisLabel "Frequency"))
                         (. chart showControls
                            false)
                         (let [results data
                               single-results (:singleresults results)
                               freqs (utils/map-keywords-to-int (:freq-hash single-results))
                               imdb-freqs (utils/map-keywords-to-int (:imdb-freq-hash single-results))
                               pair-freqs (map-to-pair-data freqs)
                               imdb-pair-freqs (map-to-pair-data imdb-freqs)]
                           (.. js/d3 (select html-svg-loc-str)
                               (datum (clj->js [{:values pair-freqs
                                                 :key "your frequency"}
                                                {:values imdb-pair-freqs
                                                 :key "imdb frequency"}]))
                               (call chart))))))))

(defn make-scatterplot! [data html-svg-loc-str]
  "NVD3 scatterChart graph.  Data-parmeter should be a map containing :singleresults
  (e.g. core.cljs/@app-state after the csv-analysis). Html-svg-loc-str is the location of the svg-element,
  (e.g. '#barchart svg')"
  (when data
    (.addGraph js/nv (fn []
                       (let [chart (.. js/nv -models scatterChart)]
                         (.showLegend chart false)
                         (.. chart -xAxis
                             (tickFormat (.format js/d3 ".0f"))
                             (axisLabel "number of movies watched"))
                         (.. chart -yAxis
                             (tickFormat (.format js/d3 ".02f"))
                             (axisLabel "Score"))
                         ;Deprecated, point value based color used instead.
                         #_(. chart color
                              (clj->js [(rgb-string 100 100 100) (rgb-string 200 200 200)] #_["rgb(0,255,0)" "rgb(255,165,0)"]))
                         (let [results data
                               single-results (:singleresults results)
                               dir-data (:dir-ranks single-results)
                               dirs-to-scatter (dir-data-to-scatter dir-data)
                               bundled-dirs-to-scatter (group-dir-scatter-duplicates dirs-to-scatter)
                               colored-dirs-to-scatter (add-color-to-scatter bundled-dirs-to-scatter :y)]
                           (.. js/d3 (select html-svg-loc-str)
                               (datum (clj->js colored-dirs-to-scatter))
                               (call chart))))))))

(defn make-h-multi-bar-chart! [data html-svg-loc-str]
  "NVD3 multiBarHorizontalChart graph. Data-parmeter should be a map containing :singleresults
  (e.g. core.cljs/@app-state after the csv-analysis). Html-svg-loc-str is the location of the svg-element,
  e.g. '#barchart svg' "
  (when data
    (.addGraph js/nv (fn []
                       (let [chart (.. js/nv -models multiBarHorizontalChart
                                       (x #(.-label %))
                                       (y #(.-value %))
                                       (margin (clj->js {:left 350}))
                                       (showControls false)
                                       (showValues true))]
                         (.. chart -tooltip (enabled false))
                         (let [results data
                               single-results (:singleresults results)
                               top-ten-disc (take 10 (:discrepancy single-results))
                               last-ten-disc (take-last 10 (:discrepancy single-results))
                               top-ten-disc-formatted (discrepancy-data-to-value-label-map top-ten-disc)
                               last-ten-disc-formatted (discrepancy-data-to-value-label-map last-ten-disc)]
                           (.. js/d3 (select html-svg-loc-str)
                               (datum (clj->js [{:values top-ten-disc-formatted
                                                 :key "top"
                                                 :color "#4f99b4"}
                                                {:values last-ten-disc-formatted
                                                 :key "bottom"
                                                 :color "#d67777"}]))
                               (call chart))))))))

(defn make-linechart! [data html-svg-loc-str]
  "NVD3 scatterChart graph with Y-values set to 0.  Data-parmeter should be a map containing :singleresults
  (e.g. core.cljs/@app-state after the csv-analysis). Html-svg-loc-str is the location of the svg-element,
  (e.g. '#barchart svg')"
  (when data
    (.addGraph js/nv (fn []
                       (let [chart (.. js/nv -models scatterChart
                                       (showYAxis false)
                                       (showLegend false))]
                         (.. chart -xAxis
                             (tickFormat (.format js/d3 ".02f"))
                             (axisLabel "Discrepancy"))
                        (let [results data
                              single-results (:singleresults results)
                              top-ten-disc (take 10 (:discrepancy single-results))
                              last-ten-disc (take-last 10 (:discrepancy single-results))
                              merged-disc-data (distinct (clojure.set/union top-ten-disc last-ten-disc))
                              formatted-disc-data (discrepancy-data-to-line-chart merged-disc-data)
                              grouped-disc-data (group-dir-scatter-duplicates formatted-disc-data)
                              colored-disc-data (add-color-to-scatter grouped-disc-data :x)]
                           (.. js/d3 (select html-svg-loc-str)
                               (datum (clj->js colored-disc-data))
                               (call chart))))))))
