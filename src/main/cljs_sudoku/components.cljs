(ns cljs-sudoku.components
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.set :refer [union difference]]
            [cljs-sudoku.sudoku :as s]
            [cljs.core.async :as async :refer [<! >! go]]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy]))

#_(defn sudoku-component [sud]
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

(def grid-style
  {:display "flex"
   :flex-flow "row wrap"
   :width "100%"
   :min-width "240px"})

(def tile-style
  {:flex-basis "calc(100%/9)"
   :border "1px solid black"
   :padding 0
   ::stylefy/mode
     {:after {:content ""
              :padding-bottom "100%"
              :display "block"}
      ":nth-child(3n):not(:nth-child(9n))"
      {:border-right "5px solid black"}
      ":nth-child(n+19):nth-child(-n+27)"
      {:border-bottom "5px solid black"}
      ":nth-child(n+46):nth-child(-n+54)"
      {:border-bottom "5px black solid"}}
                   
   
   ::stylefy/sub-styles {:content {:position "relative"
                                   :text-align :center
                                   :left 0
                                   :right 0}}})
(defn sudoku-component [sud]
  (let [highlighted (atom #{})
        highlighted-main (atom #{})
        highlighted-secondary (atom #{})
        highlighted? (atom false)
        cell-click-fn (fn [x y value]
                        (fn []
                          (when-not @highlighted?
                            (reset! highlighted? true)
                            (swap! highlighted union (s/neighbor-positions x y))
                            (swap! highlighted-secondary conj value)
                            (swap! highlighted-main conj {:x x :y y})
                            (go
                              (<! (async/timeout 750))
                              (swap! highlighted difference (s/neighbor-positions x y))
                              (swap! highlighted-main disj {:x x :y y})
                              (swap! highlighted-secondary disj value)
                              (reset! highlighted? false)))))] 
    (fn []
      (if (and sud @sud)
        [:div (stylefy/use-style grid-style)
         (doall
          (for [[i r] (map-indexed #(vector %1 %2) (:rows @sud))]
            (doall
             (for [[j n] (map-indexed #(vector %1 %2) r)]
               [:div (stylefy/use-style tile-style
                                        {:key (str "cell_" i "_" j)
                                         :class "cell"})
                 [:div (stylefy/use-sub-style
                           tile-style
                           :content
                           {:class [(when (@highlighted-main {:x j :y i})
                                      "highlighted-main")
                                    (when (@highlighted {:x j :y i})
                                      "highlighted")
                                    (when (@highlighted-secondary n)
                                      "highlighted-secondary")]
                             :on-click (cell-click-fn j i n)})
                  n]]))))]
        [:div "Click on \"generate\"."]))))


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
                                 ^{:key (str "item_" i)}
                                 [paging-item i]))
       (> 3 (- @current @start)) (doall
                                  (concat
                                   (for [i (range @start (+ 2 @current))
                                         :when (> @end i)]
                                     ^{:key (str "item_" i)}
                                     [paging-item i])
                                   [[:li {:key "elipsis_1"}
                                     [:span.pagination-ellipsis "..."]]
                                    ^{:key (str "item_" @end)}
                                    [paging-item (dec @end)]]))
       (> 3 (- @end @current)) (doall
                                (concat
                                 [^{:key "item_0"}
                                   [paging-item @start]
                                  [:li>span.ellipsis {:key "elipsis_1"} "..."]]
                                 (for [i (range (- @current 1) @end)
                                       :when (> @end i)]
                                   ^{:key (str "item_" i)}
                                   [paging-item i])))
       :else (doall
              (concat
               [^{:key "item_0"}
                [paging-item @start]
                [:li>span.pagination-ellipsis {:key "elipsis_1"} "..."]]
               (for [i (range (- @current 1) (+ @current 2))
                     :when (> @end i)]
                 ^{:key (str "item_" i)}
                 [paging-item i])
               [[:li>span.pagination-ellipsis {:key "elipsis_2"} "..."]
                ^{:key (str "item_" @end)}
                [paging-item  (dec @end)]])))]]))


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
       "view previous"
       "generate more")]]])

