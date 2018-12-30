(ns cljs-sudoku.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-event-db
 :initialize
 (fn-traced [db _]
            (-> {}
                (assoc :current-sudoku nil)
                (assoc :current-view :regular)
                (assoc :loading? false)
                (assoc :animating? false)
                (assoc :sudoku-cache {})
                (assoc :views {:regular {}
                               :past-sudokus {:current 0
                                              :start 0
                                              :end 0
                                              :sudoku nil}}))))
(rf/reg-event-db
 :init-past-sudokus-view
 (fn-traced [db _]
            (-> db
                (assoc-in [:views :past-sudokus]
                          {:current 0
                           :start 0
                           :end (count (keys (:sudoku-cache db)))
                           :sudoku (when-not (empty? (keys (:sudoku-cache db)))
                                     (->> (vals (:sudoku-cache db))
                                          (sort-by :created)
                                          (first)))}))))

(rf/reg-event-db
 :past-sudokus-set-page
 (fn-traced
  [db [_ n]]
  (-> db
      (assoc-in [:views :past-sudokus :current] n))))
(rf/reg-event-fx
 :set-current-sudoku
 (fn-traced [{:keys [db]} [_ sud id created]]
            {:db (-> db
                     (assoc :current-sudoku sud))
             :dispatch [:cache-sudoku sud id created]}))

(rf/reg-event-db
 :update-current-sudoku
 (fn-traced [db [_ sud]]
            (-> db
                (assoc :current-sudoku sud))))
(rf/reg-event-db
 :cache-sudoku
 (fn-traced
  [db [_ sud id created]]
  (-> db
      (assoc-in [:sudoku-cache id]
                {:id id
                 :sudoku sud
                 :created created}))))
(rf/reg-event-db
 :set-loading?
 (fn-traced [db [_ val]]
            (-> db
                (assoc :loading? val))))

(rf/reg-event-db
 :set-animating?
 (fn-traced [db [_ val]]
            (-> db
                (assoc :animating? val))))

(rf/reg-event-db
 :set-current-view
 (fn-traced [db [_ view]]
            (-> db
                (assoc :current-view view))))
