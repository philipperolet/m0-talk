(ns mzero.talk.chat
  (:require [mzero.talk.core :as mtc]
            [mzero.talk.log :as log]
            [clojure.string :as str]))

(def error-message "ERROR. BIP. Sorry, my brain crashed while processing what you said. My memory of this conversation was reset. Contact M0 for help about this.")

(def gpt3-completions-params
  {"api-url" "https://api.openai.com/v1/completions"
   "model" "text-davinci-003"
   "temperature" 0.2
   "max_tokens" 500
   "top_p" 1
   "frequency_penalty" 0.0
   "presence_penalty" 0.0
   "stop" ["AI:"]})

(def gpt3-chat-params
  {"api-url" "https://api.openai.com/v1/chat/completions"
   "model" "gpt-3.5-turbo"
   "temperature" 0.2
   "max_tokens" 256
   "top_p" 1
   "frequency_penalty" 0.0
   "presence_penalty" 0.0})

(defn- create-chat-prompt [{:as ai :keys [messages user-name prompt-template]}]
  (let [user-string #(if (= % "me") (str user-name ":") "AI:")
        templated-string #"\{messages\}"
        add-message-to-conversation
        (fn [message]
          (str (user-string (:user message)) " " (:text message) "\n"))
        full-conversation
        (apply str (conj (mapv add-message-to-conversation messages) "AI:"))]
    (if (re-find templated-string prompt-template)
      (str/replace-first prompt-template templated-string full-conversation)
      (str prompt-template "\n" full-conversation))))

(defn- add-message [ai message]
  (-> ai
      (update :messages #(or % []))
      (update :messages conj {:user "me" :text message})))

(defn ai-mode [ai]

  (if (re-find #"chat" (-> ai :gpt3-params (get "api-url")))
    :chat
    :completion))

(defmulti add-prompt ai-mode)

(defmethod add-prompt :completion [ai]
  (assoc-in ai [:gpt3-params "prompt"] (create-chat-prompt ai)))

(defmethod add-prompt :chat [ai]
  (let [system-message
        {"role" "system"
         "content" (ai :prompt-template)}
        convert-and-add
        (fn [messages message]
          (conj messages
                {"role" (if (= "me" (message :user)) "user" "assistant")
                 "content" (message :text)}))
        messages
        (reduce convert-and-add [system-message] (ai :messages))]
    (assoc-in ai [:gpt3-params "messages"] messages)))

(defmulti add-username-stop-token ai-mode)

(defmethod add-username-stop-token :chat [ai]
  ;; Nothing to do because stop token not needed in chat mode
  ai)

(defmethod add-username-stop-token :completion [ai]
  (update-in ai [:gpt3-params "stop"] conj (str (:user-name ai) ":")))

(defn- setup-ai-for-chat [ai message]
 (->  ai
      (add-message message)
      add-username-stop-token
      add-prompt))

(defn update-with-answer! [ai message]
  (log/info "Sending message: " message)

  (try 
    (let [chatty-ai (setup-ai-for-chat ai message)
          response @(mtc/llm-http-request! chatty-ai)
          ai-message (mtc/parse-llm-response! response (ai-mode chatty-ai))]    
      (update chatty-ai :messages conj {:user "you" :text ai-message}))
    
    (catch Exception e
      (log/info e)
      (println error-message)
      (dissoc ai message))))
