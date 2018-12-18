(ns cljs-sudoku.app
  (:require ["react"]
            [reagent.core :as reagent :refer [atom render]]
            [cljs-sudoku.sudoku :as s :refer [random-sudoku]]
            [cljs.core.async :as async :refer [<! >! chan put! go go-loop]
             :include-macros true]))

(defn generate-new [sudoku loading?]
  (fn [& opts]
    (reset! loading? true)
    (let [ch  (chan)]
      (go
        (>! ch (random-sudoku)))
      (go-loop [res (<! ch)]
        (if (= :ok (:result res))
          (reset! sudoku (:data res))
          (.log js/console "Error generating"))
        (reset! loading? false)))))

(defn sudoku-component [sud]
  (if @sud
    [:div.sudoku.card
     (for [[i r] (map-indexed #(vector %1 %2) (:rows @sud))]
       [:div.columns.is-mobile.is-gapless.is-centered>div.column
        {:key (str "row_" i)}
        (for [[j n] (map-indexed #(vector %1 %2) r)]
          [:span.cell
           {:key (str "cell_" i "_" j)}
           n])])]
    [:div "Apreta el boton"]))

(defn generate-btn [sudoku loading?]
      [:a
       {:on-click (generate-new sudoku loading?)
        :class ["button" "is-large" "is-primary"
                "is-fullwidth" (when @loading? "is-loading")]}
       "quiero mas!"])

(defn navbar [sudoku loading?]
  [:nav.navbar.is-primary
   {:role :navigation}
   [:div.navbar-brand
    [:div.navbar-item
     [:h1
      "sudoku"]]
    #_[:div.navbar-item.is-pulled-right
     [:button.button.is-primary
      {:type :button
       :on-click (generate-new sudoku loading?)
       :class ["button"
               "is-secondary"
               (when @loading?
                 "is-loading")]}
      "Generar"]]]
   [:div.navbar-menu.is-active
    [:div.navbar-end
     [:div.navbar-item
      [:button.button.is-primary
       {:type :button
        :on-click (generate-new sudoku loading?)
        :class ["button"
                "is-secondary"
                (when @loading?
                  "is-loading")]}
       "Generar"]]]]])


(defn app [sudoku loading?]
  [:div.app
   [navbar sudoku loading?]
   [:div.section
    [:div.columns.is-mobile.is-gapless>div.column.is-three-fifths-tablet.is-centered.is-full-mobile
     (when @sudoku
       [sudoku-component sudoku])]]])

(defn mount-components [sudoku loading?]
  (render
   [app sudoku loading?]
   (.getElementById js/document "root")))

(defn init! []
  (let [sudoku   (atom nil)
        loading? (atom false)]
    (mount-components sudoku loading?)))
