(ns mzero.talk.chat
  (:require [mzero.talk.core :as mtc]
            [mzero.talk.log :as log]))

(def error-message "ERROR. BIP. Sorry, my brain crashed while processing what you said. My memory of this conversation was reset. Contact M0 for help about this.")

(def gpt3-chat-params
  {"model" "text-davinci-003"
   "temperature" 0.2
   "max_tokens" 500
   "top_p" 1
   "frequency_penalty" 0.0
   "presence_penalty" 0.0
   "stop" ["AI:"]})

(defn- create-chat-prompt [{:as ai :keys [messages user-name prompt-template]}]
  (let [user-string #(if (= % "me") (str user-name ":") "AI:")        
        add-message-to-conversation
        (fn [message]
          (str (user-string (:user message)) " " (:text message) "\n"))]
    (apply str prompt-template "\n" (conj (mapv add-message-to-conversation messages) "AI:"))))

(defn- add-message [ai message]
  (-> ai
      (update :messages #(or % []))
      (update :messages conj {:user "me" :text message})))

(defn- setup-ai-for-chat [ai message]
  (let [ai-with-message (add-message ai message)
        default-chat-params
        (update gpt3-chat-params "stop" conj (str (:user-name ai) ":"))]
    (-> ai-with-message
        (update :gpt3-params #(or % default-chat-params))
        (assoc-in [:gpt3-params "prompt"] (create-chat-prompt ai-with-message)))))

(defn update-with-answer! [ai message]
  (log/info "Sending message: " message)

  (try 
    (let [chatty-ai (setup-ai-for-chat ai message)
          response @(mtc/llm-http-request! chatty-ai)
          ai-message (mtc/parse-llm-response! response)]    
      (update chatty-ai :messages conj {:user "you" :text ai-message}))
    
    (catch Exception e
      (log/info e)
      (println error-message)
      (dissoc ai message))))
