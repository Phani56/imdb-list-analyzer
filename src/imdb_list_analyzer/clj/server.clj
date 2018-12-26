(ns imdb-list-analyzer.clj.server
  (:gen-class)
  (:require [imdb-list-analyzer.clj.imdb-data :as imdb]
            [imdb-list-analyzer.clj.result-view :as resview]
            [compojure.core :refer [GET POST defroutes routes context]]
            [compojure.handler :refer [site api]]
            [compojure.route :refer [resources not-found files]]
            [ring.util.response :refer [response status resource-response]]
            [ring.middleware.multipart-params :as multiparams]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.java.io :as io]))

;TODO needed to do non-dependent version from core to avoid cyclic dependency
(defn imdb-file-analysis [file]
  (resview/compute-results
    (rest (imdb/parse-imdb-data (imdb/read-raw-data file)))))

(defn handle-hello-req []
  (do
    (println "hello req succes!")
    ;Response
    (str "Hello from the server-side!")))

(defn handle-csv-req [request]
  (do
    (println "csv req success!")
    (println request)
    ;(println (core/one-file-analysis (:tempfile (:csv (:params request)))))
    ;Response
    (resview/jsonify-single-result
      (imdb-file-analysis (:tempfile (:csv (:params request)))))))

(defn handle-example-req [request]
  (do
    (println "example req success!")
    (println request)
    ;Response
    (resview/jsonify-single-result
      (imdb-file-analysis (io/resource "example_ratings_2018_format_A.csv")))))

(defroutes site-routes
           (GET "/" [] (resource-response "index.html" {:root "public"}))
           (POST "/hello" [] (handle-hello-req))
           (POST "/analyze" req (handle-csv-req req))
           (POST "/analyze-example" req (handle-example-req req))
           (resources "/")
           (not-found "Page not found"))

(def sites
  (site site-routes))

(def app
  (-> (routes sites
       multiparams/wrap-multipart-params)))

(defn start-server []
  (run-jetty #'app {:port 3000}))
