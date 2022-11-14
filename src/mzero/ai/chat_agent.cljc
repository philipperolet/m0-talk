(ns mzero.ai.chat-agent
  (:require [clojure.spec.alpha :as s]))

(s/def ::prompt-init string?)
(s/def ::agent (s/keys :req-un [::prompt-init ::messages ::user-name]))
