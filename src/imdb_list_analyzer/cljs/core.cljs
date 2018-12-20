(ns ^:figwheel-always ^:figwheel-hooks imdb-list-analyzer.cljs.core
  (:require
    [imdb-list-analyzer.cljs.utils :as utils]
    [imdb-list-analyzer.cljs.graphs :as graphs]
    [reagent.core :as r]
    [ajax.core :refer [GET POST]]
    [goog.dom :as gdom]))

(enable-console-print!)

(defonce app-state (r/atom nil))

(defonce dom-state (r/atom {:loading false
                            :error nil
                            :file nil}))

(defn result-handler [response]
    (do
      #_(println (-> response (js/JSON.parse) (js->clj :keywordize-keys true))) ;For debug
      (swap! dom-state assoc :loading false)
      (reset! app-state (-> response (js/JSON.parse) (js->clj :keywordize-keys true)))
      (graphs/make-histogram! @app-state "#barchart svg")
      (graphs/make-h-multi-bar-chart! @app-state "#h-multi-bar-chart svg")
      (graphs/make-linechart! @app-state "#line-chart svg")
      (graphs/make-scatterplot! @app-state "#scatterplot svg")))

(defn error-handler [{:keys [status status-text]}]
  (do
    (swap! dom-state assoc :error (str status " " status-text ". Is the file a valid imdb csv-file?"))
    (swap! dom-state assoc :loading false)))

(defn histogram-component []
  [:div {:id "barchart" :style {:width 1000 :height 400}}
   [:svg]])

(defn scatterplot-component []
  [:div {:id "scatterplot" :style {:width 1000 :height 400}}
   [:svg]])

(defn h-multi-bar-chart-component []
  [:div {:id "h-multi-bar-chart" :style {:width 1000 :height 400}}
   [:svg]])

(defn line-chart-component []
  [:div {:id "line-chart" :style {:width 1000 :height 150}}
   [:svg]])

(defn inst-component []
  [:div.container
   [:h2 "IMDB list analyzer"]
   [:br]
   [:div
      [:h4 "Anyone with an IMDb account can retrieve their own ratings file as follows:"]
      [:li "Login to www.imdb.com with you account."]
      [:li "Search for a personal \"Your Ratings\" view that contains all your rated movies."]
      [:li "Click \"Export this list\" at the bottom of the page."]
      [:li "Save file into the filesystem."]
      [:li "Use 'Choose file' & 'Analyze file' buttons to analyze your ratings"]]])

(defn form-component []
  [:div.container
   [:h3 "Load your ratings:"]
   [:form {:encType "multipart/form-data"
           :method "post"
           :name "csv-form"
           :id "csv-form"}
    [:input {:class "btn btn-default btn-file"
             :type "file" :name "csv"
             :id "csv-input" :accept ".csv"
             ;:value @file
             :on-change
              #(swap! dom-state assoc :file (.-name (aget (.-files (.getElementById js/document "csv-input")) 0)))}]
    [:br]
    [:div {:class "inline"}
     [:input  {:class "btn btn-primary"
               :type "button"
               :value "Analyze file"
               :disabled (or
                          (:loading @dom-state)
                          (nil? (:file @dom-state)))
               :onClick #(do
                           (swap! dom-state assoc :loading true)
                           (swap! dom-state assoc :error nil)
                           (POST "/analyze"
                                 {:body (js/FormData.
                                          (.getElementById js/document "csv-form"))
                                  :handler result-handler
                                  :error-handler error-handler}))}]
     [:br]
     [:br]
     [:input  {:class "btn btn-default"
               :type "button"
               :value "Show example results"
               :disabled (:loading @dom-state)
               :onClick #(do
                          (swap! dom-state assoc :loading true)
                          (swap! dom-state assoc :error nil)
                          (POST "/analyze-example"
                                {:handler result-handler
                                 :error-handler error-handler}))}]
     [:div.loading {:hidden (not (:loading @dom-state))}
        [:i {:class "fa fa-cog fa-spin fa-4x"}]]]]
   [:div {:class "container"}
    [:h4 (:error @dom-state)]]])

(defn result-component []
  (let [results @app-state
        single-results (:singleresults results)
        freqs (utils/map-keywords-to-int (:freq-hash single-results))
        imdb-freqs (utils/map-keywords-to-int (:imdb-freq-hash single-results))
        best-dirs (take 10 (:dir-ranks single-results))
        worst-dirs (take-last 10 (:dir-ranks single-results))]
    [:div.container {:id "results-elem"
                     :hidden (nil? results)}
     [:h3 "IMDB single-list analysis results"]
     [:table.table
       [:thead
        [:tr
         [:th "Metric"]
         [:th "Result"]
         [:th "IMDB Average"]]]
       [:tbody
        [:tr
         [:td "Number of movie ratings"]
         [:td (str (:num single-results))]
         [:td ""]]
        [:tr
         [:td "Mean of movie ratings"]
         [:td (str (utils/round-num (:mean single-results) 2))]
         [:td (str (utils/round-num (:imdb-mean single-results) 2))]]
        [:tr
         [:td "Standard deviation of movie ratings"]
         [:td (str (utils/round-num (:stdev single-results) 2))]
         [:td (str (utils/round-num (:imdb-stdev single-results) 2))]]
        [:tr
         [:td "Correlation between ratings and IMDb rating averages"]
         [:td (str (utils/round-num (:corr single-results) 2))]
         [:td ""]]]]
     [:h3 "Frequencies of ratings"]
     [histogram-component]

     ;Button to re-render the histogram, as the graph is sometimes bugged on first load
     [:button {:class  "btn btn-default"
                :onClick #(graphs/make-histogram! @app-state "#barchart svg")}
       "Re-render graph"]

     [:h3 "Directors: Watched movies and score"]
     [scatterplot-component]
     [:button {:class  "btn btn-default"
               :onClick #(graphs/make-scatterplot! @app-state "#scatterplot svg")}
      "Re-render graph"]

     [:h3 "The best directors"]
     [:table.table
        [:thead
         [:tr
          [:th "Director-name"]
          [:th "Score"]
          [:th "Rates"]]]
        [:tbody
         (for [dir-data best-dirs
               :let [dir (str (first (first dir-data)))
                     rates (str (last (first dir-data)))
                     p-value (str (last dir-data))]]
           ^{:key dir}
           [:tr
            [:td dir]
            [:td p-value]
            [:td rates]])]]

     [:h3 "The worst directors"]
     ;TODO remove repeating code by making a list-directors function
     [:table.table
      [:thead
       [:tr
        [:th "Director-name"]
        [:th "Score"]
        [:th "Rates"]]]
      [:tbody
       (for [dir-data worst-dirs
             :let [dir (str (first (first dir-data)))
                   rates (str (last (first dir-data)))
                   p-value (str (last dir-data))]]
         ^{:key dir}
         [:tr
          [:td dir]
          [:td p-value]
          [:td rates]])]]

     [:h3 "Movies with unexpected ratings, Chart 1"]
     [h-multi-bar-chart-component]
     [:button {:class  "btn btn-default"
               :onClick #(graphs/make-h-multi-bar-chart! @app-state "#h-multi-bar-chart svg")}
      "Re-render graph"]

     [:h3 "Movies with unexpected ratings, Chart 2"]
     [line-chart-component]
     [:button {:class  "btn btn-default"
               :onClick #(graphs/make-linechart! @app-state "#line-chart svg")}
      "Re-render graph"]]))

(defn root-component []
  [:div.container
    [inst-component]
    [form-component]
    [result-component]])

(defn ^:export ^:after-load run []
  (r/render [root-component]
            (.getElementById js/document "app")))
