(ns capstone-cljs-front-end.app
  (:require [capstone-cljs-front-end.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
