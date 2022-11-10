(ns mzero.web.run
  (:require  [mzero.web.chat :as c]
             [cljs-http.client :as http]
             [reagent.dom :refer [render]]
             [clojure.core.async :refer [<!] :refer-macros [go]]
             [goog.dom :as gdom]
             [clojure.string :as str]))

(defn to-json-str
  "Convert to JSON string with namespaced keywords"
  [data]
  (.stringify js/JSON (clj->js data :keyword-fn #(subs (str %) 1))))

(defn from-json-str
  "Opposite of to-json-str"
  [json-str]
  (js->clj (.parse js/JSON json-str) :keywordize-keys true))

(def m0-talk-handler-url "https://guyltmkjuacodlhyyoim5vprli0uxien.lambda-url.eu-west-3.on.aws/")

(def gpt3-params-template
  ;; encode for security
  (js/btoa
   {"model" "text-davinci-002"
    "temperature" 0.5
    "max_tokens" 300
    "top_p" 1
    "frequency_penalty" 0.0
    "presence_penalty" 0.6
    "stop" [" Human:" " AI:"]}))

(def prompt-init
  (str "The following is a conversation between a human and an AI."
       "The machine is nice, intelligent and well spoken.\n"))

(defn- create-prompt [messages]
  (let [user-name #(if (= % "me") "Human:" "AI:")        
        add-message-to-conversation
        (fn [message]
          (str (user-name (:user message)) " " (:text message) "\n"))]
    (apply str prompt-init (mapcat add-message-to-conversation messages))))

(defn- llm-http-request! [messages]
  (let [prompt (create-prompt messages)
        headers {"content-type" "text/plain"}
        body
        (-> gpt3-params-template
            ;; decode param template
            js/atob cljs.reader/read-string
            (assoc "prompt" prompt))]
    (http/post m0-talk-handler-url
               {:with-credentials? false
                :body (js/btoa (to-json-str body))
                :headers headers})))

(defn- parse-llm-response [response]
  (-> response :body from-json-str
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
