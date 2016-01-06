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

(defonce counter (reagent/atom 0))

(defonce test-info (reagent/atom (sorted-map)))

(defn add-api-info []
  (let [id (swap! counter inc)]
    (swap! test-info assoc id {:id id
                               :address nil 
                               :name nil
                               :payload nil
                               :action nil})))
;; (defn add-todo [text]
;;   (let [id (swap! counter inc)]
;;     (swap! todos assoc id {:id id :title text :done false})))


(defn minus-api-info [id]
  (swap! test-info dissoc id))

(defonce init (do
                (add-api-info)))

(defn api-form [test-info n]
  (fn [] 
    [:div.row
     [:div.col-md-8
      [:input {:type "radio" :name "action" :value "POST"
               :on-click #(swap! test-info assoc-in  [n :action] (.-target.value %))} "POST "]
      [:input {:type "radio" :name "action" :value "GET" 
               :on-click #(swap! test-info assoc-in  [n :action] (.-target.value %))} "GET "]
      [:button.btn.btn-default.btn-sm.pull-right {:on-click #(minus-api-info n)}
       [:span.glyphicon.glyphicon-minus]]
      [:p]
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
      [:p]
      [:div {:style {:visibility ((fn []
                                    (if (= (get-in @test-info [n :action]) "POST")
                                      (str "visible")
                                      (str "hidden")))) }}
       [:label "Payload for POST"]
       [:div.input-group 
        [:textarea.form-control {:type "textarea"
                                 :rows 5
                                 :value (get-in @test-info [n :payload])
                                 :on-change #(swap! test-info assoc-in  [n :payload] (.-target.value %))}]]]]]))


(defn add-test []
   [:div.col-md-8
    [:button.btn.btn-default.btn-sm.pull-right {:on-click add-api-info}
     [:span.glyphicon.glyphicon-plus]]])


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
    (for [single-api-test @test-info]
      [:div.row
       ^{:key (:id single-api-test)}
       [api-form test-info @counter]])
    [:div.row
     [add-test]]]])

;;[api-and-name test-info (- (count @test-info) 1)
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
