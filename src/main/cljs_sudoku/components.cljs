(ns cljs-sudoku.components
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.set :refer [union difference]]
            [cljs-sudoku.sudoku :as s]
            [cljs.core.async :as async :refer [<! >! go]]
            [re-frame.core :as rf]))

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
      (if (and sud @sud)
        [:div.sudoku.card
         (doall
          (for [[i r] (map-indexed #(vector %1 %2) (:rows @sud))]
            [:div.columns.is-mobile.is-gapless.is-centered
             {:key (str "row_" i)}
             [:div.column
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
                  n]))]]))]
        [:div "Apreta el boton"]))))


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


(defn navbar []
  [:nav.navbar.is-primary
   {:role :navigation}
   [:div.navbar-brand
    [:div.navbar-item
     [:h1
      "sudoku"]]
    [:div.navbar-item>a
     {:on-click #(if (= @(rf/subscribe [:current-view]) :regular)
                   (rf/dispatch [:set-current-view :history])
                   (rf/dispatch [:set-current-view :regular]))}
     (if (= @(rf/subscribe [:current-view]) :regular)
       "ver anteriores"
       "generá más")]]])

