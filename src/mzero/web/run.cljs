(ns mzero.web.run
  (:require  [mzero.web.chat :as c]
             [cljs-http.client :as http]
             [reagent.dom :refer [render]]
             [clojure.core.async :refer [<!] :refer-macros [go]]
             [goog.dom :as gdom]
             [clojure.string :as str]))
(def error-message "Sorry, my brain crashed trying to understand what you said. Please restart the conversation by reloading this page.")

(def prompt-init
  "The following is a conversation between a user and an AI assistant:")

(defn mobile-device? []
  (re-find #"Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini"
           (.-userAgent js/navigator)))

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
   {"model" "text-davinci-003"
    "temperature" 0.2
    "max_tokens" 1000
    "top_p" 1
    "frequency_penalty" 0.0
    "presence_penalty" 0.6
    "stop" ["User:" "AI:"]}))

(defn- create-prompt [messages]
  (let [user-name #(if (= % "me") "User:" "AI:")        
        add-message-to-conversation
        (fn [message]
          (str (user-name (:user message)) " " (:text message) "\n"))]
    (apply str prompt-init (conj (mapv add-message-to-conversation messages) "AI:"))))

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

(defn- parse-llm-response
  "Return AI's message, or nil if AI failed to respond"
  [response]
  (-> response :body from-json-str
      :choices first :text))

(defn- show-loading! []
  ;; disable user message sending
  (doall
   (map #(set! (.-disabled %) true)
        (.querySelectorAll js/document ".mzero-chat .new-message *")))
  ;; show loading
  (-> (.querySelector js/document ".mzero-chat")
      .-classList
      (.add "loading")))

(defn- hide-loading! []
  (-> (.querySelector js/document ".mzero-chat")
      .-classList
      (.remove "loading")))

(defn- enable-input! []
  (doall
   (map #(set! (.-disabled %) false)
        (.querySelectorAll js/document ".mzero-chat .new-message *"))))

(defn- resume-chat! []
  (hide-loading!)
  (enable-input!)
  ;; focus on input text except on mobile
  (when (not (mobile-device?))
    (.focus (.querySelector js/document ".mzero-chat .new-message input")))) 

(defn- talk-back! [_]
  (show-loading!)
  (go (try
        (let [response (<! (llm-http-request! (:messages @c/chat-data)))
              ai-message (parse-llm-response response)]
          (when (<= 400 (:status response))
            (throw (js/Error. "Request failed to lambda")))
          (resume-chat!)
          (c/send-message "you" ai-message))
        (catch js/Error e
          (hide-loading!)
          (c/send-message "you" error-message)
          (throw e)))))

(defn- start-ai-conversation []
  (set! c/send-button-callback talk-back!))

(defn- run []
  (reset! c/chat-data {:messages []})
  (render [c/chat-component] (gdom/getElement "m0-talk") start-ai-conversation))

(run)
