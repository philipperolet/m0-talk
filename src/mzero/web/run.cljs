(ns mzero.web.run
  (:require  [mzero.web.chat :as c]
             [cljs-http.client :as http]
             [reagent.dom :refer [render]]
             [clojure.core.async :refer [<!] :refer-macros [go]]
             [goog.dom :as gdom]
             [clojure.string :as str]))

(def openai-completions-api-url "https://api.openai.com/v1/completions")

(def gpt3-params-template
  {"model" "text-davinci-002"
   "prompt" nil
   "temperature" 0.5
   "max_tokens" 300
   "top_p" 1
   "frequency_penalty" 0.0
   "presence_penalty" 0.6
   "stop" [" Human:" " AI:"]})

(def prompt-init
  (str "The following is a conversation between a human and an AI."
       "The machine is nice, intelligent and well spoken.\n"))

(defn to-json-str
  "Convert to JSON string with namespaced keywords"
  [data]
  (.stringify js/JSON (clj->js data :keyword-fn #(subs (str %) 1))))

(defn- create-prompt [messages]
  (let [user-name #(if (= % "me") "Human:" "AI:")        
        add-message-to-conversation
        (fn [message]
          (str (user-name (:user message)) " " (:text message) "\n"))]
    (apply str prompt-init (mapcat add-message-to-conversation messages))))

(defn- llm-http-request! [messages]
  (let [prompt (create-prompt messages)
        gpt3-params (assoc gpt3-params-template "prompt" prompt)
        headers
        {"Authorization"
         "Bearer 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"
         "content-type" "application/json"}]
    (http/post openai-completions-api-url
               {:with-credentials? false
                :body (to-json-str gpt3-params)
                :headers headers})))

(defn- parse-llm-response [response]
  (-> response :body
      :choices first :text
      ;; remove the 'AI:' part from the text
      (str/split "AI:") second))
(defn- talk-back! [message]
  (go (let [response (<! (llm-http-request! (:messages @c/chat-data)))]
        (c/send-message "you" (parse-llm-response response)))))

(defn- start-ai-conversation []
  (c/send-message "you" "Hi, I'm an AI and I'd like to chat.")
  (set! c/send-button-callback talk-back!))

(defn- run []
  (render [c/chat-component] (gdom/getElement "m0-talk") start-ai-conversation))

(run)
