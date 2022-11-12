(ns mzero.web.run
  (:require  [mzero.web.chat :as c]
             [cljs-http.client :as http]
             [reagent.dom :refer [render]]
             [clojure.core.async :refer [<!] :refer-macros [go]]
             [goog.dom :as gdom]
             [clojure.string :as str]))

(def prompt-init
  "Jean-Noel Barrot is Secretary of Technology under French President Emmanuel Macron's administration. He is working with the Secretary of Economy Bruno Le Maire in order to ensure that France develops the best policies regarding digital and technological innovation. 

The following are conversations between Jean-Noel and his personal AI assistant. The assistant can do lots of things, such as help him with technological matters, give advice about Jean-Noel's job as Secretary of Technology, answer any kind of question, or just have a casual chat. The assistant is often cynical and sarcastic, but overall very insightful and his advice is good. Sometimes, Jean-Noel says weird things but the assistant finds a witty response. The first conversation, shown below, is short:
AI: Hello Jean-Noel.
Jean-Noel: Can you help me with something?
AI: Sure. As usual.
Jean-Noel: I don't remember the name of the president's wife.
AI: It's Brigitte. I'm unclear how you could forget that.
Jean-Noel: ok thanks

The second one is short too and focused on a specific policy:
AI: Hello Mr. Secretary. Do you need something?
Jean-Noel: Yes, I'm working on a new policy to improve technological skills of young children by introducing coding lessons at school. How would you name such a policy?
AI: You could name it \"Teach the Future\" or more soberly \"Digital Literacy Act\". Although I know you're not always on the sober side.
Jean-Noel: I like \"Teach the Future\". At what age should children learn programming in your opinion?
AI: Probably around 8 years old. Before that, they won't be able to understand properly the abstractions you can find in programming languages.
Jean-Noel: Right. Thanks.
AI: Of course.

The third conversation is longer. It goes like this:
")

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
    "temperature" 0.9
    "max_tokens" 300
    "top_p" 1
    "frequency_penalty" 0.0
    "presence_penalty" 0.6
    "stop" ["Jean-Noel:" "AI:"]}))

(defn- create-prompt [messages]
  (let [user-name #(if (= % "me") "Jean-Noel:" "AI:")        
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

(defn- parse-llm-response [response]
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
  ;; hide loading
  (-> (.querySelector js/document ".mzero-chat")
      .-classList
      (.remove "loading"))
  ;; enable user message sending
  (doall
   (map #(set! (.-disabled %) false)
        (.querySelectorAll js/document ".mzero-chat .new-message *")))
  ;; focus on input text
  #_(.focus (.querySelector js/document ".mzero-chat .new-message input"))) 

(defn- talk-back! [_]
  (show-loading!)
  (go (let [response (<! (llm-http-request! (:messages @c/chat-data)))]
        (hide-loading!)
        (c/send-message "you" (parse-llm-response response)))))

(defn- start-ai-conversation []
  (talk-back! nil)
  (set! c/send-button-callback talk-back!))

(defn- run []
  (reset! c/chat-data {:messages []})
  (render [c/chat-component] (gdom/getElement "m0-talk") start-ai-conversation))

(run)
