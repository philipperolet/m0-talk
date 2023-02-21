(ns mzero.talk.utils
  (:require #?(:clj [clojure.data.json :as json])))

(defn to-json-str
  "Convert to JSON string with namespaced keywords"
  [data]
  #?(:clj (json/write-str data)
     :cljs (.stringify js/JSON (clj->js data :keyword-fn #(subs (str %) 1)))))

(defn from-json-str
  "Opposite of to-json-str"
  [json-str]
  #?(:clj (json/read-str json-str :key-fn keyword)
     :cljs (js->clj (.parse js/JSON json-str) :keywordize-keys true)))

