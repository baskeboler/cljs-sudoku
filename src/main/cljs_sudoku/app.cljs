(ns cljs-sudoku.app
  (:require ["react"]
            [reagent.core :as reagent :refer [atom render]]
            [cljs-sudoku.sudoku :as s :refer [random-sudoku neighbor-positions]]
            [cljs.core.async :as async :refer [<! >! chan put! go go-loop]
             :include-macros true]))

(def current-view (atom :regular))
            
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

(defn pagination
  [start end current fn-page]
  [:nav.pagination
   {:role :pagination
    :aria-label :pagination}
   [:a.pagination-previous {:on-click #(if (> @current @start) (fn-page (dec @current)))}
    "previous"]
   [:a.pagination-next {:on-click #(if (< @current @end) (fn-page (inc @current)))}
    "next"]
   [:ul.pagination-list
    (doall
     (for [i (range @start @end)]
      [:li>a
       {:key (str "item_" i)
        :class ["pagination-link" (when (= @current i) "is-current")]
        :on-click #(fn-page i)}
       i]))]])
(defn sudoku-component [sud]
  (let [highlighted (atom #{})]
    (fn []
      (if @sud
        [:div.sudoku.card
         (doall
          (for [[i r] (map-indexed #(vector %1 %2) (:rows @sud))]
           [:div.columns.is-mobile.is-gapless.is-centered>div.column
            {:key (str "row_" i)}
            (doall
             (for [[j n] (map-indexed #(vector %1 %2) r)]
              [:span.cell
               {:key (str "cell_" i "_" j)
                :class ["cell" (when (@highlighted {:x j :y i}) "highlighted")]
                :on-click #(do
                             (reset! highlighted (s/neighbor-positions j i))
                             (go
                               (<! (async/timeout 2000))
                               (reset! highlighted #{})))}
               n]))]))]
        [:div "Apreta el boton"]))))

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
    [:div.navbar-item>a
     {:on-click #(reset! current-view :regular)}
     "regular"]
    [:div.navbar-item>a
     {:on-click #(reset! current-view :history)}
     "history"]
    #_[:div.navbar-item.is-pulled-right
        [:button.button.is-primary
          {:type :button
           :on-click (generate-new sudoku loading?)
           :class ["button"]
               "is-secondary"
               (when @loading?
                 "is-loading")}
         "Generar"]]]
   #_[:div.navbar-menu.is-active
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

(defn past-sudokus-view []
  (let [sudoku-vec (->> (vals @s/sudokus)
                        (sort-by :created)
                        (map :sudoku)
                        (into []))
        start (atom 0)
        end (atom (count sudoku-vec))
        current (atom 0)
        current-sudoku (atom (nth sudoku-vec @current))
        page-fn (fn [p]
                  (cond
                    (< p @start) (reset! current @start)
                    (>= p @end) (reset! current (dec @end))
                    :else (reset! current p))
                  (reset! current-sudoku (nth sudoku-vec @current)))]
    (fn []
      [:div.section
       [pagination start end current page-fn]
       [sudoku-component current-sudoku]])))

(defn regular-view [sudoku loading?]
  [:div.section
    [:div.columns.is-mobile.is-gapless>div.column.is-centered.is-full-mobile
     [:button
      {:type :button
       :on-click (generate-new sudoku loading?)
       :class ["button"
               "is-fullwidth"
               "is-warning"
               (when @loading?
                 "is-loading")]}
      "Generar"]
     [:hr]
     (when @sudoku
        [sudoku-component sudoku])]])
(defn app [sudoku loading?]
  [:div.app
   [navbar sudoku loading?]
   (cond
     (= @current-view :regular) [regular-view sudoku loading?]
     (= @current-view :history) [past-sudokus-view])])

(defn mount-components [sudoku loading?]
  (render
   [app sudoku loading?]
   (.getElementById js/document "root")))

(defn init! []
  (let [sudoku   (atom nil)
        loading? (atom false)]
    (mount-components sudoku loading?)))
