(ns mzero.talk.log
  (:require #?(:clj [clojure.tools.logging :as log])))


(def info
  #?(:clj log/info
     :cljs
     (fn [& info-args]
       (.log (js/console) "INFO: " msgs))))


