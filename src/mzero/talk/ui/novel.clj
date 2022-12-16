(ns mzero.talk.ui.novel
  (:require [mzero.talk.core :as mtc]))

(def output-file "../wp21/answer.txt")
(def gpt3-params
  {"model" "text-davinci-003"
   "temperature" 0.4
   "max_tokens" 500
   "top_p" 1
   "frequency_penalty" 1.0
   "presence_penalty" 1.0})

(def ai
  {:gpt3-params gpt3-params})

(defn- get-prompt [& files]
  (let [files
        (->> (or files ["base-prompt.txt"])
             (map #(str "../wp21/" %)))]
    (apply str (map slurp files))))

(defn answer [message]
  (-> ai
      (assoc-in [:gpt3-params "prompt"] message)
      mtc/llm-http-request!
      mtc/wait-for-response!
      mtc/parse-llm-response!))

(defn ans [message] (println (answer message)))


(defn prompt [& files]
  (spit output-file (answer (apply get-prompt files))))

(defn prm [& files]
  (spit (last files) (answer (apply get-prompt files)) :append true))

(defn go-on [& files]
  (spit output-file (answer (str (apply get-prompt files)  (slurp output-file))) :append true))
