(ns capstone-cljs-front-end.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[capstone-cljs-front-end started successfully]=-"))
   :middleware identity})
