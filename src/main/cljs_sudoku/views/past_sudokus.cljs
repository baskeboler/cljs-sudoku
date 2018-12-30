(ns cljs-sudoku.views.past-sudokus
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [cljs-sudoku.components :refer [sudoku-component pagination]]
            [cljs-sudoku.sudoku :as s]))


(rf/reg-sub
 :past-sudokus-view
 (fn [db _]
   (get-in db [:views :past-sudokus])))

(rf/reg-sub
 :past-sudokus
 (fn [db _]
   (->> (vals (:sudoku-cache db))
        (vec)
        (sort-by :created)
        (mapv :sudoku))))

(rf/reg-sub
 :past-sudokus-current-sudoku
 :<- [:past-sudokus-current-index]
 :<- [:past-sudokus]
 (fn [[idx sudokus-vec] _]
   (nth sudokus-vec idx nil)))

(rf/reg-sub
 :past-sudokus-start
 :<- [:past-sudokus-view]
 (fn [view _]
   (:start view)))

(rf/reg-sub
 :past-sudokus-end
 :<- [:past-sudokus]
 (fn [sudokus _]
   (count sudokus)))

(rf/reg-sub
 :past-sudokus-current-index
 :<- [:past-sudokus-view]
 (fn [view _]
   (:current view)))


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
