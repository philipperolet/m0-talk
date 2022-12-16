(ns mzero.talk.chat
  (:require [mzero.talk.core :as mtc]))

(def error-message "Sorry, my brain crashed trying to understand what you said. Please restart the conversation by reloading this page.")

(def gpt3-chat-params
  {"model" "text-davinci-003"
   "temperature" 0.7
   "max_tokens" 2000
   "top_p" 1
   "frequency_penalty" 0.9
   "presence_penalty" 0.9
   "stop" ["AI:"]})

(defn- create-chat-prompt [{:as ai :keys [messages user-name prompt-init]}]
  (let [user-string #(if (= % "me") (str user-name ":") "AI:")        
        add-message-to-conversation
        (fn [message]
          (str (user-string (:user message)) " " (:text message) "\n"))]
    (apply str prompt-init "\n" (conj (mapv add-message-to-conversation messages) "AI:"))))

(defn- add-message [ai message]
  (-> ai
      (update :messages #(or % []))
      (update :messages conj {:user "me" :text message})))

(defn- setup-ai-for-chat [ai message]
  (let [ai-with-message (add-message ai message)]
    (-> ai-with-message
        (update :gpt3-params #(or % gpt3-chat-params))
        (assoc-in [:gpt3-params "prompt"] (create-chat-prompt ai-with-message))
        (update-in [:gpt3-params "stop"] conj (str (:user-name ai) ":")))))

(defn update-with-answer! [ai message]
  (let [chatty-ai (setup-ai-for-chat ai message)
        response @(mtc/llm-http-request! chatty-ai)
        ai-message (mtc/parse-llm-response! response)]    
    (update chatty-ai :messages conj {:user "you" :text ai-message})))
