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

(defn minus-api-info [id]
  (swap! test-info dissoc id)) 

(defn getTest [id]
  (get @test-info id))

(defn getName [id]
  (get-in @test-info [id :name]))

(defn add-api-test! []
    (swap! test-info assoc-in [:testing id] {:id id
                                             :address nil 
                                             :name nil
                                             :payload nil
                                             :action nil}))

(defn set-editing! [& test-id]
  (let [id (swap! counter inc)]
    (if (nil? test-id)
      (swap! test-info assoc :editing {:id id
                                       :address nil 
                                       :name nil
                                       :payload nil
                                       :action nil})
      (swap! test-info assoc :editing (getTest test-id)))))


(defonce init (do
                (set-editing!)))

;; (defn chartist-bar []
;;   (let [data (clj->js {:labels (array 'mon' 'wed' 'tues' 'thurs' 'fri') :series (array (array 1 2 3 4 5))}) options (clj->js {:distrobuteSeries true})]
;;     (println (.Line js/Chartist ".ct-chart" data))
;;     ;;(.Linex js/Chartist ".chart1" data)
;;   (fn []
;;     [:h2 (str (.Line js/Chartist ".ct-chart" data))]
;;     [:div.ct-chart.ct-golden-section {:id "chart1"}])))

(defn bar-for-chart [perf label]
  [:div.row
   [:p label]
   [:div.progress
    [:div.progress-bar.progress-bar-striped.active {:style
                                                    {
                                                     ;; :height (str "10%")
                                                     :width (str (:percentage perf) "%")
                                                     ;; :background-color "grey"
                                                    }}] (str "Avg. Requests / second " (:velocity perf))]])



(defn add-test-btn []
  ;; functionality needs to be added here to append new-test into the app-db and set :editing @test-info to new (empty) api obj  
  [:div.col-md-8
   [:button.btn.btn-default.btn-lg.pull-right {:on-click add-api-test!}
    [:span.glyphicon.glyphicon-plus] (str " Api To Test List")]])

(defn rm-test-btn [id]
  [:button.btn.btn-default.btn-md.pull-right {:on-click #(minus-api-info id)}
   [:span.glyphicon.glyphicon-remove]])

(defn api-form []
  ;; (println "ID: " id)
  (let [id (:editing @test-info)]
    (fn []
      [:div.row
      [:p]
       [:div.col-md-8
        [:div.btn-group {:role "group" :id (str "action-" id)}
         [:button.btn.btn-default {:type "button"
                                   :name "get-btn"
                                   :value "GET"
                                   :on-click #(swap! test-info assoc-in [id :action] (.-target.value %)
                                                     ())} "GET"]

         [:button.btn.btn-default {:type "button" :value "POST" :on-click #(swap! test-info assoc-in [id :action] (.-target.value %))} "POST"]]
        
        ;;[rm-test-btn id]
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
                                   :on-change #(swap! test-info assoc-in  [id :payload] (.-target.value %))}]]]]])))

(defn set-editing! [id]
  (println "EDITING TEST INFO: " (:editing @test-info))
  (swap! test-info assoc :editing id))

(defn name-capsule [id]
  [:div.row 
   [:col-md-10 {:on-click #(set-editing! id)} (str (getName id))]
   [:col-md-2 [rm-test-btn id]]])

(defn home-page []
  [:div.container
   [:div.row
    [:div.col-md-12
    [:div.jumbotron
     [:h1 "Welcome to API Velociraptor"]]]]
   [:div.row
    [:h2 {:style {:background-color "tomato"}} (str "TEST INFO: " @test-info " counter: " @counter " EDITING: " (:editing @test-info))]]
   [:div.row
    [:div.col-md-2 {:style {:background-color "blue" :color "white" :font-size "20px"}}
     (doall (for [test1 @test-info]
             ^{:key (first test1)} [name-capsule (first test1)]))]
    [:div.col-md-10
     [:h2 "API Info"]
      [api-form]
    [:div.row
     [:p]
     [add-test-btn]]]]])


(defn results []
  [:div.container
   ;; [chartist-bar]
   [:div.row
    [:h2 "THE RESULTS PAGE"]
    [:div.well
     [bar-for-chart {:percentage 10 :velocity 45} "This Awesome API bar"]
     [bar-for-chart {:percentage 20 :velocity 68} "This also an bar"]
     [bar-for-chart {:percentage 50 :velocity 180} "This is now a bar"]]
    ]])

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
