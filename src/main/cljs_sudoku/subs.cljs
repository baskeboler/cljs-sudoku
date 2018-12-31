(ns cljs-sudoku.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :current-view
 (fn [db _] (:current-view db)))

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

(rf/reg-sub
 :current-sudoku
 (fn [db _] (:current-sudoku db)))

(rf/reg-sub
 :loading?
 (fn [db _] (:loading? db)))

(rf/reg-sub
 :animating?
 (fn [db _] (:animating? db)))
;; (def current-view (rf/subscribe [:current-view])))


(rf/reg-sub
 :play-view
 (fn [db _]
   (get-in db [:views :play])))

(rf/reg-sub
 :sudoku-ids
 (fn [db _]
   (->> (get db :sudoku-cache)
        (keys)
        (into []))))

(rf/reg-sub
 :navbar-items
 (fn [db _]
   (->> (get-in db [:navbar :items])
        (into []))))

(rf/reg-sub
 :navbar-menu-active?
 (fn [db _]
   (get-in db [:navbar :active?])))

(rf/reg-sub
 :current-play-sudoku-index
 :<- [:play-view]
 (fn [view _]
   (:current view)))

(rf/reg-sub
 :current-play-solution
 :<- [:current-play-sudoku-index]
 :<- [:past-sudokus]
 (fn [[idx sudokus-vec] _]
   (nth sudokus-vec idx)))

(rf/reg-sub
 :current-game
 :<- [:play-view]
 (fn [view _]
   (:current-game view)))

(rf/reg-sub
 :current-game-var-count
 :<- [:play-view]
 (fn [view _]
   (:var-count view)))

(rf/reg-sub
 :current-game-vars
 :<- [:current-game]
 (fn [game _]
   (->> (:vars game)
        (into #{}))))
