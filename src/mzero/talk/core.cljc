(ns mzero.talk.core
  (:require  [org.httpkit.client :as http]
             [clojure.data.json :as json]
             [clojure.tools.logging :as log]
             [clojure.core.async :refer [<!] :refer-macros [go]]
             [clojure.string :as str]))

(def error-message "Sorry, my brain crashed trying to understand what you said. Please restart the conversation by reloading this page.")

(def ai-api-url "https://api.openai.com/v1/completions")
(def gpt3-params-template
  {"model" "text-davinci-002"
   "temperature" 0.9
   "max_tokens" 300
   "top_p" 1
   "frequency_penalty" 0.0
   "presence_penalty" 0.6
   "stop" ["AI:"]})

(defn- create-prompt [{:as ai :keys [messages user-name prompt-init]}]
  (let [user-string #(if (= % "me") (str user-name ":") "AI:")        
        add-message-to-conversation
        (fn [message]
          (str (user-string (:user message)) " " (:text message) "\n"))]
    (apply str prompt-init "\n" (conj (mapv add-message-to-conversation messages) "AI:"))))

(defn- llm-http-request! [{:as ai :keys [messages user-name]}]
  (let [prompt (create-prompt ai)
        headers {"content-type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}
        body
        (-> gpt3-params-template
            (assoc "prompt" prompt)
            (update "stop" conj (str user-name ":")))]
    (http/post ai-api-url
               {:body (json/write-str body)
                :headers headers})))

(defn- parse-llm-response
  "Return AI's message, or nil if AI failed to respond"
  [response]
  (-> response :body (#(json/read-str % :key-fn keyword))
      :choices first :text))

(defn- add-message [ai message]
  (-> ai
      (update :messages #(or % []))
      (update :messages conj {:user "me" :text message})))

(defn update-with-answer! [ai message]
  (let [ai-with-message (add-message ai message)
        response @(llm-http-request! ai-with-message)
        ai-message (parse-llm-response response)]
    (when (>= (:status response) 400)
      (throw (ex-info "Request failed." response)))
    (update ai-with-message :messages conj {:user "you" :text ai-message})))







