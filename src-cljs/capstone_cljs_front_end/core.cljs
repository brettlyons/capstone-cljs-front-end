(ns capstone-cljs-front-end.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [cljsjs.chartist]
            ;; [cljsjs.d3]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  [:li {:class (when (= page (session/get :page)) "active")}
   [:a {:href uri
        :on-click #(reset! collapsed? true)}
    title]])

(defn navbar []
  (let [collapsed? (atom true)]
    (fn []
      [:nav.navbar.navbar-inverse.navbar-fixed-top
       [:div.container
        [:div.navbar-header
         [:button.navbar-toggle
          {:class         (when-not @collapsed? "collapsed")
           :data-toggle   "collapse"
           :aria-expanded @collapsed?
           :aria-controls "navbar"
           :on-click      #(swap! collapsed? not)}
          [:span.sr-only "Toggle Navigation"]
          [:span.icon-bar]
          [:span.icon-bar]
          [:span.icon-bar]]
         [:a.navbar-brand {:href "#/"} "API Velociraptor"]]
        [:div.navbar-collapse.collapse
         (when-not @collapsed? {:class "in"})
         [:ul.nav.navbar-nav
          [nav-link "#/" "Home" :home collapsed?]
          [nav-link "#/results" "Current Results" :results collapsed?]
          [nav-link "#/about" "About" :about collapsed?]]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "This app is for defining load-testing targets and showing load-testing results"]]])

(defonce counter (reagent/atom 0))
(defonce test-info (reagent/atom {}))

(defn add-api-info []
  (let [id (swap! counter inc)]
    (swap! test-info assoc id {:id id
                               :address nil 
                               :name nil
                               :payload nil
                               :action nil})))

(defn minus-api-info [id]
  (swap! test-info dissoc id)) 

(defonce init (do
                (add-api-info)))

;; (defn chartist-bar []
;;   (let [data (clj->js {:labels (array 'mon' 'wed' 'tues' 'thurs' 'fri') :series (array (array 1 2 3 4 5))}) options (clj->js {:distrobuteSeries true})]
;;     (println (.Line js/Chartist "#chart1" data "#chart1"))
;;     (.Line js/Chartist "#chart1" data "#chart1")
;;   (fn []
;;     [:h2 (str (.Line js/Chartist "#chart1" data "#chart1"))]
;;     [:div.ct-chart.ct-golden-section {:id "chart1"}])))

(defn bar-for-chart [percentage label]
  [:div.row
    [:p label]
    [:div.progress
     [:div.progress-bar {:style {:width (str percentage "%")}}]]])

(defn add-test-btn []
   [:div.col-md-8
    [:button.btn.btn-default.btn-sm.pull-right {:on-click add-api-info}
     [:span.glyphicon.glyphicon-plus]]])

(defn rm-test-btn [id]
      [:button.btn.btn-default.btn-sm.pull-right {:on-click #(minus-api-info id)}
       [:span.glyphicon.glyphicon-minus]])

(defn api-form [id]
  ;; (println "ID: " id)
  (fn [] 
    [:div.row
     [:div.col-md-8
      [:input {:type "radio" :name (str "action-" id) :value "POST"
               :on-click #(swap! test-info assoc-in [id :action] (.-target.value %))} "POST "]
      [:input {:type "radio" :name (str "action-" id) :value "GET" 
               :on-click #(swap! test-info assoc-in [id :action] (.-target.value %))} "GET "]
      [rm-test-btn id]
      [:p]
      [:label "Full URL"]
      [:div.input-group
       [:span.input-group-addon "Url: "]
       [:input.form-control {:type "URL"
                             :value (get-in @test-info [id :address])
                             :on-change #(swap! test-info assoc-in [id :address] (.-target.value %))}]]


      [:label "Name of the API to appear on the graph"]
      [:div.input-group
       [:span.input-group-addon "Name: "]
       [:input.form-control {:type "text"
                             :value (get-in @test-info [id :name])
                             :on-change #(swap! test-info assoc-in [id :name] (.-target.value %))}]]]
     [:div.col-md-4
      [:p]
      [:div {:style {:visibility ((fn []
                                    (if (= (get-in @test-info [id :action]) "POST")
                                      "visible"
                                      "hidden"))) }}
       [:label "Payload for POST"]
       [:div.input-group 
        [:textarea.form-control {:type "textarea"
                                 :rows 5
                                 :value (get-in @test-info [id :payload])
                                 :on-change #(swap! test-info assoc-in  [id :payload] (.-target.value %))}]]]]]))


(defn home-page []
    [:div.container
     [:div.jumbotron
      [:h1 "Welcome to API Velociraptor"]]]
  [:div.container
   [:div.row
    [:h2 (str "TEST INFO: " @test-info " counter: " @counter)]]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to API Velociraptor"]]
    (doall (for [test @test-info]
       ^{:key (first test)} [api-form (first test)]))
    [:div.row
     [add-test-btn]]]])

(defn results []
  [:div.container
   [chartist-bar]
   [:div.row
    [:div.well
     [bar-for-chart 10 "This Awesome API bar"]
     [bar-for-chart 20 "This also an bar"]
     [bar-for-chart 50 "This is now a bar"]]
    [:h2 "THE RESULTS PAGE"]]])

(def pages
  {:home #'home-page
   :results #'results
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/results" []
  (session/put! :page :results))

(secretary/defroute "/about" []
  (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (reagent/render [#'navbar] (.getElementById js/document "navbar"))
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
