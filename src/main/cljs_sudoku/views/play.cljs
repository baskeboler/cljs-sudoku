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

(defn play-view []
  (let [ids (rf/subscribe [:sudoku-ids])
        game (rf/subscribe [:current-game])]
    [:div.section.play-sudoku
     [:h3 "Let's play"]
     (when-not @game
       [:button.button.is-warning
        {:type :button
         :on-click generate-game}
        "get game"])
     (when @game
       [game-component game])
     [:hr]
     [:ul
       (for [i @ids]
         [:li {:key (str "item_" i)}
          i])]]))
