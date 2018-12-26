(defproject imdb-list-analyzer "0.1.0-SNAPSHOT"
  :description "Tool for analyzing IMDb rating lists"
  :url "http://github.com/dresa/imdb-list-analyzer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.csv "0.1.4"]  ; parsing CSV strings
                 [cheshire "5.5.0"] ; JSON functions
                 [org.clojure/clojurescript "1.10.439"]
                 [reagent "0.8.1"]
                 [compojure "1.4.0"]
                 [ring/ring-core "1.7.0"]
                 [ring/ring-jetty-adapter "1.7.0"]
                 [cljs-ajax "0.5.1"]
                 [com.bhauman/figwheel-main "0.2.0-SNAPSHOT"]
                 [com.bhauman/rebel-readline-cljs "0.1.4"]]

  :plugins       [[lein-ring "0.9.7"]
                  [lein-cljsbuild "1.1.7"]]

  ; for figwheel
  ;:resource-paths ["target" "resources"] ; this will break lein uberjar, moved to profiles
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "build-dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]}

  :clean-targets ^{:protect false} ["target" "resources/public/cljs-out"]
  :profiles {:uberjar
             {:omit-source true
              :prep-tasks  ["compile" ["cljsbuild" "once" "prod"]]
              :aot :all
              :cljsbuild   {
                            :builds {:prod {
                                            :source-paths ["src/imdb_list_analyzer/cljs"]
                                            :compiler     {
                                                           :output-to "resources/public/cljs-out/dev-main.js"
                                                           ;TODO :adavanced optimization do not work for some reason
                                                           :optimizations :whitespace
                                                           :pretty-print  false}}}}
              :uberjar-name "imdb-list-analyzer-standalone.jar"
              :source-paths ["src/imdb_list_analyzer/clj"]
              :resource-paths ["resources/public"]}
             :figwheel {:resource-paths ["target" "resources"]}}



  :min-lein-version "2.0.0"
  :main imdb-list-analyzer.clj.core)


