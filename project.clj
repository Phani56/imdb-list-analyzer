(defproject imdb-list-analyzer "0.1.0-SNAPSHOT"
  :description "Tool for analyzing IMDb rating lists"
  :url "http://github.com/dresa/imdb-list-analyzer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.csv "0.1.4"]  ; parsing CSV strings
                 [cheshire "5.5.0"] ; JSON functions
                 [org.clojure/clojurescript "1.10.339"]
                 [reagent "0.5.1"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "provided"]
                 [compojure "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [cljs-ajax "0.5.1"]]

  :plugins       [[lein-ring "0.9.7"]
                  [lein-cljsbuild "1.1.7"]]

  :ring {:handler imdb-list-analyzer.server/app}

  :profiles {:production
             {:hooks [leiningen.cljsbuild]
              :cljsbuild {
                          :builds [{
                                    :source-paths ["src"]
                                    :compiler {
                                               :id "prod"
                                               :output-to "resources/public/js/main.js"  ; default: target/cljsbuild-main.js
                                               :output-dir "resources/public/js"
                                               :figwheel false
                                               :optimizations :whitespace
                                               :pretty-print false}}]}
              :ring {
                     :open-browser? false
                     :stacktraces? false
                     :auto-reload? false}}}

  :min-lein-version "2.0.0"
  :main imdb-list-analyzer.core
  :aot [imdb-list-analyzer.core])
