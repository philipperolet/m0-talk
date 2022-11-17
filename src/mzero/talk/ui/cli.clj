(ns mzero.talk.ui.cli
  "Command-line interface to talk to the agent"
  (:require [mzero.talk.core :as mtc])
  (:gen-class))

(def input-chars ":> ")

(defn talk-repl! [ai]
  (print input-chars)
  (flush)
  (when-let [input (read-line)]
    (let [updated-ai (mtc/update-with-answer! ai input)
          ai-answer (-> updated-ai :messages last :text)]
      (println ai-answer)
      (recur updated-ai))))


(def ai
  {:prompt-init "The following is a conversation between a human named Philippe and his AI assistant. The assistant is very helpful, clever and insightful, and always find a ways to answer Philippe's questions or fulfill his demands."
   :user-name "Philippe"})

(defn -main []
  (talk-repl! ai))

