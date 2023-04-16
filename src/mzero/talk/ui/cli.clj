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

(def ai-modes-params
  {:completion mtct/gpt3-completions-params
   :chat mtct/gpt3-chat-params
   :gpt4 (assoc mtct/gpt3-chat-params "model" "gpt-4")})

(def ai
  {:prompt-template "You are an AI assistant chatting with a person called Philippe, living in Paris. You give concise answers, with examples if needed. This is your conversation.\n"
   :user-name "Philippe"
   :gpt3-params (ai-modes-params :gpt4)})

(defn mode-command [ai-atom input]
  (let [mode (second (re-find #"!!mode ([-\w]*)[^-\w]*" input))
        new-params (ai-modes-params (keyword mode))]
    (when new-params
      (swap! ai-atom assoc :gpt3-params new-params))
    (if new-params
      (str "Switching to " mode " mode")
      (str "Mode " mode " unknown. Doing nothing."))))

(defn ai-repl-eval
  "Either run a special command, specified by an initial `!!`, or query
  the chat agent and display its answer"
  [ai-atom input]
  (if (re-matches #"^!!.*" input)
    ;; special command
    (case (second (re-find #"!!(reset|no-op|mode)[^\w]*" input))
      "reset"
      (do (swap! ai-atom dissoc :messages) "COMMAND: reset. Chat history reset.")
      
      "no-op"
      "COMMAND: no-op. Doing nothing."

      "mode"
      (str "COMMAND: mode. " (mode-command ai-atom input))      
      
      nil
      "ERROR: unknown command.")
    
    ;; query ai chat agent
    (->> (mtct/update-with-answer! @ai-atom input)
         (reset! ai-atom)
         :messages last :text)))

(def repl-options
  (let [ai-atom (atom ai)]
    {:prompt #(printf ":> ")
     :read
     (fn [request-prompt request-exit]
       (or ({:line-start request-prompt :stream-end request-exit}
            (main/skip-whitespace *in*))
           (read-line)))
     :eval (partial ai-repl-eval ai-atom)
     :print #(println %)}))

(defn ai-repl []
  (apply repl (flatten (seq repl-options))))

(defn -main []
  (ai-repl))
