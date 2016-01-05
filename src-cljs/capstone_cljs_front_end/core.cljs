(ns capstone-cljs-front-end.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
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
          [nav-link "#/about" "About" :about collapsed?]]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "This app is for defining load-testing targets and showing load-testing results"]]])

(def api-address (reagent/atom nil))
(def api-name (reagent/atom nil))
(def api-payload (reagent/atom nil))
(def test-info (reagent/atom [{:address "BLAH 1"
                               :name nil
                               :payload nil
                               :action "GET"}]))

;; (defn increment-count [c]
;;     (swap! app-state update-in [:counters (:id c) :count] inc))

(defn toggle-action
  [action]
  (if (= action "GET")
    (str "POST")
    (str "GET")))

(defn api-and-name [test-info n]
  (fn [] 
    [:div.row
     [:h2 (str "TEST INFO: " @test-info " n: " n " Get-In test: " (get-in @test-info [n :address]))]
     [:div.col-md-8
      ;; [:button.btn.btn-default {:on-click #(swap! test-info assoc-in [n :action] (toggle-action (get-in @test-info [n :action]))) } (str "Current Method: "(get-in @test-info [n :action])) ]
      ;; set up boot-switch here for checkboxs
      [:br]
      [:label "Full URL"]
      [:div.input-group
       [:span.input-group-addon "Url: "]
       [:input.form-control {:type "URL"
                             :value (get-in @test-info [n :address])
                             :on-change #(swap! test-info assoc-in  [n :address] (.-target.value %))}]]


      [:label "Name of the API to appear on the graph"]
      [:div.input-group
       [:span.input-group-addon "Name: "]
       [:input.form-control {:type "text"
                             :value (get-in @test-info [n :name])
                             :on-change #(swap! test-info assoc-in  [n :name] (.-target.value %))}]]]
     [:div.col-md-4
      [:label "JSON payload to send"]
      [:div.input-group
       [:textarea.form-control {:type "textarea"
                                :rows 4
                                :value (get-in @test-info [n :payload])
                                :on-change #(swap! test-info assoc-in  [n :payload] (.-target.value %))}]]]]))


(defn add-api-info [counter]
  (println "Add-info hit, counter" counter)
  (swap! counter inc))

(defn minus-api-info [counter]
  (println "minus-info hit, counter" counter)
  (swap! counter dec))

(defn add-test [counter]
  [:div.row
   [:button.btn.btn-default.btn-sm {:on-click #(add-api-info counter)}
    [:span.glyphicon.glyphicon-plus]]
   [:button.btn.btn-default.btn-sm {:on-click #(minus-api-info counter)}
    [:span.glyphicon.glyphicon-minus]]])


(defn home-page []
  (let [counter (reagent/atom 0)]
    [:div.container
     [:div.jumbotron
      [:h1 "Welcome to API Velociraptor"]]]
    [:div.container
     [:div.row
      [:div.col-md-12
       [:h2 "Welcome to API Velociraptor"]]]
     [:div.row
      [api-and-name test-info @counter]]
     [:div.row
      [add-test counter]]]))

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

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
