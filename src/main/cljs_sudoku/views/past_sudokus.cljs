(ns cljs-sudoku.views.past-sudokus
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [cljs-sudoku.components :refer [sudoku-component pagination]]
            [cljs-sudoku.sudoku :as s]))



(defn past-sudokus-view []
  (let [view-data (rf/subscribe [:past-sudokus-view])
        start (rf/subscribe [:past-sudokus-start])
        end (rf/subscribe [:past-sudokus-end])
        current (rf/subscribe [:past-sudokus-current-index])
        current-sudoku (rf/subscribe [:past-sudokus-current-sudoku])
        page-fn (fn [p]
                  (rf/dispatch [:past-sudokus-set-page p]))]
    (fn []
      [:div.section
       [pagination start end current page-fn]
       [:hr]
       [sudoku-component current-sudoku]])))
