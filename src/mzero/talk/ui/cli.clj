(ns mzero.talk.ui.cli
  "Command-line interface to talk to the agent"
  (:require [mzero.talk.chat :as mtct]
            [clojure.main :as main :refer [repl]])
  (:gen-class))

(def input-chars ":> ")

(defn talk-repl! [ai]
  (print input-chars)
  (flush)
  (when-let [input (read-line)]
    (let [updated-ai (mtct/update-with-answer! ai input)
          ai-answer (-> updated-ai :messages last :text)]
      (println ai-answer)
      (recur updated-ai))))


(def ai
  {:prompt-init "The following is a conversation between a human named Philippe and his AI assistant. The assistant is very helpful, clever and insightful. It is also very honest and earnest. When it doesn't know the answer to a question, or what to do about Philippe requests, it says so to Philippe.\n"
   :user-name "Philippe"})

(def repl-options
  (let [ai-atom (atom (assoc ai :prompt-init ""))]
    {:prompt #(printf "PR) ")
     :read
     (fn [request-prompt request-exit]
       (or ({:line-start request-prompt :stream-end request-exit}
            (main/skip-whitespace *in*))
           (read-line)))
     :eval (fn [input]
             (->> (mtct/update-with-answer! @ai-atom input)
                  (reset! ai-atom)
                  :messages last :text))
     :print #(println "G)" %)}))

(defn ai-repl []
  (apply repl (flatten (seq repl-options))))

(defn -main []
  (talk-repl! ai))
