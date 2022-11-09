(ns mzero.web.run
  (:require  [mzero.web.chat :as chat]
             [reagent.dom :refer [render]]
             [goog.dom :as gdom]))

(defn- run []
  (render [chat/chat-component] (gdom/getElement "m0-talk")))

(run)
