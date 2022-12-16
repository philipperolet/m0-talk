(ns mzero.talk.core
  (:require  [org.httpkit.client :as http]
             [clojure.data.json :as json]
             [clojure.tools.logging :as log]
             [clojure.core.async :refer [<!] :refer-macros [go]]
             [clojure.string :as str]))

(def ai-api-url "https://api.openai.com/v1/completions")

(defn llm-http-request! [{:as ai :keys [gpt3-params]}]
  (log/info "Sending request with params:\n" (dissoc gpt3-params "prompt"))
  (let [headers {"content-type" "application/json"
                 "Authorization" (str "Bearer " (System/getenv "OPENAI_API_KEY"))}]
    (http/post ai-api-url
               {:body (json/write-str gpt3-params)
                :headers headers})))

(defn parse-llm-response
  "Return AI's message, or nil if AI failed to respond"
  [response]
  (-> response :body (#(json/read-str % :key-fn keyword))
      :choices first :text))

(defn parse-llm-response! [response]
  (log/info response)
  (when (>= (:status response) 400)
    (throw (ex-info "Request failed." response)))
  (let [response (update response :body #(json/read-str % :key-fn keyword))]
    (log/info "Response: "
              (-> (update response :body dissoc :choices)
                  (update :opts dissoc :body)))
    (log/info (-> response :body :usage)
              (-> response :headers :openai-processing-ms) "ms"))
  (parse-llm-response response))

(defn wait-for-response! [llm-promise]
  (log/info "Waiting for server response")
  (while (not (realized? llm-promise))
    (Thread/sleep 500)
    (print ".")
    (flush))
  @llm-promise)
