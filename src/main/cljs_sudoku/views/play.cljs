(ns cljs-sudoku.views.play
  (:require [cljs-sudoku.components :refer [game-component]]
            [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [cljs-sudoku.sudoku :as s]))

(defn generate-game []
  (let [n @(rf/subscribe [:current-game-var-count])
        sol @(rf/subscribe [:current-play-solution])
        game-result (s/generate-game sol n)]
    (when-not (= :error (:status game-result))
      (rf/dispatch [:set-current-game (:game game-result)]))))

(defn select-var-count []
  (let [n (rf/subscribe [:current-game-var-count])]
    [:div.select
     [:select
      {:on-change #(rf/dispatch [:set-game-var-count
                                 (js/Number.parseInt
                                  (-> % .-target .-value))])}
      (for [i (range 5 30)]
        [:option {:key (str "option_" i)
                  :selected (if (= i @n) true false)
                  :value i}
         i])]]))

(defn input-control-fn [x y new-number]
  (if (and (>= new-number 1)
           (<= new-number 9))
    (rf/dispatch [:set-game-var-value x y new-number])
    (rf/dispatch [:set-game-var-value x y 0])))

(defn winning-modal []
  (let [active? (atom true)]
    (fn []
      [:div.modal {:class (if @active? ["is-active"] [])}
       [:div.modal-background]
       [:div.modal-content
        [:p.image.is-4by3
         [:img {:src "images/success.gif"}]]]
       [:button.modal-close.is-large {:aria-label "close"
                                      :on-click #(reset! active? false)}]])))

(defn play-view []
  (let [ids (rf/subscribe [:sudoku-ids])
        game (rf/subscribe [:current-game])
        solved? (rf/subscribe [:current-game-solved?])]
    [:div.section.play-sudoku
     [:h3 "Let's play"]
     [select-var-count]
     (when (and @game @solved?)
       [winning-modal])
     [:button.button.is-warning
      {:type :button
       :on-click generate-game}
      "get game"]
     (when @game
       [game-component game input-control-fn])
     [:hr]
     #_[:ul
        (for [i @ids]
          [:li {:key (str "item_" i)}
           i])]]))
