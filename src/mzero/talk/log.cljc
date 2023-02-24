(ns mzero.talk.log
  (:require #?(:clj [clojure.tools.logging :as log])))


#?(:clj
   (defmacro info [message & info-args] `(log/info ~message ~@info-args))
   :cljs
   (def info (fn [& info-args]
               (.log (js/console) "INFO: " msgs))))


