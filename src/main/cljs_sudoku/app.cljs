(ns cljs-sudoku.app
  (:require ["react"]
            [reagent.core :as reagent :refer [atom render]]
            [clojure.set :as sets :refer [difference union]]
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
  (let [paging-item (fn [page-num]
                      [:li>a
                       {
                        :class ["pagination-link" (when (= @current page-num) "is-current")]
                        :on-click #(fn-page page-num)}
                       page-num])]
   [:nav.pagination
    {:role :pagination
     :aria-label :pagination}
    [:a.pagination-previous {:on-click #(if (> @current @start) (fn-page (dec @current)))}
     "previous"]
    [:a.pagination-next {:on-click #(if (< @current @end) (fn-page (inc @current)))}
     "next"]
    [:ul.pagination-list
     (cond
       (> 10 (- @end @start)) (doall
                               (for [i (range @start @end)]
                                 [paging-item i {:key (str "item_" i)}]))
       (> 3 (- @current @start)) (doall
                                  (concat
                                   (for [i (range @start (+ 2 @current))
                                         :when (> @end i)]
                                     [paging-item i {:key (str "item_" i)}])
                                   [[:li
                                     [:span.pagination-ellipsis "..."]]
                                    [paging-item (dec @end)]]))
       (> 3 (- @end @current)) (doall
                                (concat
                                 [[paging-item @start]
                                  [:li>span.ellipsis "..."]]
                                 (for [i (range (- @current 1) @end)
                                       :when (> @end i)]
                                   [paging-item i {:key (str "item_" i)}])))
       :else (doall
              (concat
               [[paging-item @start]
                [:li>span.pagination-ellipsis "..."]]
               (for [i (range (- @current 1) (+ @current 2))
                     :when (> @end i)]
                 [paging-item i {:key (str "item_" i)}])
               [[:li>span.pagination-ellipsis "..."]
                [paging-item (dec @end)]])))]]))

(defn sudoku-component [sud]
  (let [highlighted (atom #{})
        highlighted-secondary (atom #{})
        cell-click-fn (fn [x y value]
                        (fn []
                          (swap! highlighted union (s/neighbor-positions x y))
                          (swap! highlighted-secondary conj value)
                          (go
                            (<! (async/timeout 2000))
                            (swap! highlighted difference (s/neighbor-positions x y))
                            (swap! highlighted-secondary disj value))))] 
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
                :class ["cell"
                        (when (@highlighted {:x j :y i})
                          "highlighted")
                        (when (@highlighted-secondary n)
                          "highlighted-secondary")]
                :on-click (cell-click-fn j i n)}
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
     {:on-click #(if (= @current-view :regular)
                   (reset! current-view :history)
                   (reset! current-view :regular))}
     (if (= @current-view :regular) "ver anteriores" "generá más")]]])

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
       [:hr]
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
