(ns capstone-cljs-front-end.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [capstone-cljs-front-end.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[capstone-cljs-front-end started successfully using the development profile]=-"))
   :middleware wrap-dev})
