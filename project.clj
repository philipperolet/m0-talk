(defproject org.clojars.philipperolet/m0-talk "0.1"
  :description "An interface to talk to an AI"
  :url "https://github.com/philipperolet/m0-talk"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.758"]
                 [org.clojure/data.json "2.4.0"]
                 [http-kit "2.6.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [reagent "1.1.1"]
                 [cljs-http "0.1.46"]
                 [org.clojars.philipperolet/cljs-chat "0.4.0"]]
  :source-paths ["src"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:prod"   ["run" "-m" "figwheel.main" "--output-to" "resources/public/js/m0-talk.js" "-O" "advanced" "-bo" "dev"]}
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]
  :main mzero.talk.ui.cli
  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.18"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]}})
