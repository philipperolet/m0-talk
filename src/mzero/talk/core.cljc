(ns mzero.talk.core
  (:require #?(:clj [org.httpkit.client :as http]
               :cljs [cljs-http.client :as http])
            #?(:clj [org.httpkit.sni-client :as sni-client])
            [mzero.talk.log :as log]
            [mzero.talk.utils :refer [from-json-str to-json-str]]
            [clojure.core.async :refer [<!] :refer-macros [go]]
            [clojure.string :as str]))


(alter-var-root #'org.httpkit.client/*default-client*
                (fn [_] sni-client/default-client))

(def openai-api-key 
  #?(:clj (System/getenv "OPENAI_API_KEY")
     :cljs (js/m0talkOpenaiKey)))

(defn llm-http-request! [{:as ai  {:as gpt3-params :strs [api-url]} :gpt3-params}]
  (log/info "Sending request with params:\n" (dissoc gpt3-params "prompt"))
  (let [headers {"content-type" "application/json"
                 "Authorization" (str "Bearer " openai-api-key)}]
    (http/post api-url
               {:body (to-json-str (dissoc gpt3-params "api-url"))
                :headers headers})))

(defn parse-llm-response
  "Return AI's message, or nil if AI failed to respond.

  `mode` can be either `:chat` or `:completion` depending on the api used"
  ([response mode]
   (let [choice (-> response :body from-json-str :choices first)]
     (if (= mode :completion)
       (choice :text)
       (-> choice :message :content))))
  ([response]
   (parse-llm-response response :completion)))

(defn parse-llm-response!
  ([response mode]
   (log/info response)
   (when (>= (:status response) 400)
     (throw (ex-info "Request failed." response)))
   (let [response (update response :body from-json-str)]
     (log/info "Response: "
               (-> (update response :body dissoc :choices)
                   (update :opts dissoc :body)))
     (log/info (-> response :body :usage)
               (-> response :headers :openai-processing-ms) "ms"))
   (parse-llm-response response mode))
  ([response] (parse-llm-response! response :completion)))

#?(:clj
   (defn wait-for-response! [llm-promise]
     (log/info "Waiting for server response")
     (while (not (realized? llm-promise))
       (Thread/sleep 500)
       (print ".")
       (flush))
     @llm-promise))
