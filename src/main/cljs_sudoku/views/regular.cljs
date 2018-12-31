(ns cljs-sudoku.views.regular
  (:require [re-frame.core :as rf]
            [cljs-sudoku.components :refer [sudoku-component]]
            [cljs.core.async :as async :refer [<! >! go chan]]
            [cljs-sudoku.sudoku :as s :refer [random-sudoku]]))




(defn animate-transition
  "animates the transition between one sudoku board and
  another"
  [new-sudoku]
  (let [sudoku (rf/subscribe [:current-sudoku])
        busy? (rf/subscribe [:animating?])]
    (let [anim-chan (chan 82)
          cell-changes (into [] (for [i (range 9) j (range 9)]
                                  {:x i
                                   :y j
                                   :dt (* 10 (+ (*  9 j) i))
                                   :new-val (s/get-position new-sudoku i j)}))
          finish-timeout (async/timeout 2000)]
      (rf/dispatch-sync [:set-animating? true])
      (doseq [change cell-changes]
        (let [to-chan (async/timeout (+ 190 (:dt change)))]
          (go
            (<! to-chan)
            (rf/dispatch-sync [:update-current-sudoku
                               (-> @(rf/subscribe [:current-sudoku])
                                   (s/set-position
                                    (:x change)
                                    (:y change)
                                    (:new-val change)))]))))
      (go
        (<! finish-timeout)
        (rf/dispatch [:set-animating? false])
        (rf/dispatch-sync [:set-current-sudoku new-sudoku (random-uuid) (js/Date.)])))))

(defn generate-new []
  (let [sudoku (rf/subscribe [:current-sudoku])
        loading? (rf/subscribe [:loading?])
        aniamting? (rf/subscribe [:animating?])]
   (fn [& opts]
     (rf/dispatch [:set-loading? true])
     (let [ch  (chan)]
       (go
         (>! ch (random-sudoku)))
       (go
         (let [res (<! ch)
               created (js/Date.)
               id (random-uuid)]
           (if (= :ok (:result res))
             ;; (reset! sudoku (:data res))
             (if-not @sudoku
               (rf/dispatch [:set-current-sudoku (:data res) id created])
               (animate-transition (:data res)))
             (.log js/console "Error generating"))
           (rf/dispatch [:set-loading? false])))))))


(defn generate-btn []
      [:a
       {:on-click (generate-new)
        :class ["button" "is-large" "is-primary"
                "is-fullwidth" (when @(rf/subscribe [:loading?]) "is-loading")]}
       "More!"])


(defn regular-view []
  [:div.section
   [:div.columns.is-mobile.is-gapless>div.column.is-centered.is-full-mobile
    [:button
     {:type :button
      :on-click (generate-new)
      :class ["button"
              "is-fullwidth"
              "is-warning"
              (when @(rf/subscribe [:loading?])
                "is-loading")]}
     "Generate"]
    [:hr]
    [sudoku-component (rf/subscribe [:current-sudoku])]]])
