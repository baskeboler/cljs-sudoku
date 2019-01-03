(ns cljs-sudoku.views.play
  (:require [cljs-sudoku.components :refer [game-component]]
            [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [cljs-sudoku.sudoku :as s]
            [cljs.core.async :as async :refer [<! >! go chan]]))

(defn generate-game []
  (let [n @(rf/subscribe [:current-game-var-count])
        sol @(rf/subscribe [:current-play-solution])
        game-result (s/generate-game sol n)]
    (if-not (= :error (:status game-result))
      (rf/dispatch [:set-current-game (:game game-result)])
      (js/console.log "Could not find valid game. Booooo!"))))

(defn generate-solution-and-game []
  (let [n @(rf/subscribe [:current-game-var-count])
        solution-result (s/random-sudoku)
        sol-chan (chan)]
    (rf/dispatch-sync [:set-game-is-generating true])
    (go
      (let [result (<! sol-chan)]
        (if (= :ok (:status result))
          (rf/dispatch [:set-current-sudoku-and-game
                        (:sudoku result)
                        (random-uuid)
                        (js/Date.)
                        (:game result)])
          (js/console.log (:message result)))))
    (go
      (if (= :ok (:result solution-result))
        (let [game-result (s/generate-game (:data solution-result) n)]
          (if (= :ok (:status game-result))
            (>! sol-chan  {:status :ok
                           :sudoku (:data solution-result)
                           :game (:game game-result)})
            (>! sol-chan {:status :error
                          :message "Failed to generate game."})))
        (>! sol-chan  {:status :error
                       :message "Failed to generate solution."}))
      (rf/dispatch [:set-game-is-generating false]))))

(defn select-var-count []
  (let [n (rf/subscribe [:current-game-var-count])]
    [:div.field.has-addons
     [:div.control
      [:a.button.is-static "# empty cells: "]]
     [:div.control.is-expanded
      [:div.select.is-fullwidth
       [:select
        {:value @n
         :on-change #(rf/dispatch [:set-game-var-count
                                   (js/Number.parseInt
                                    (-> % .-target .-value))])}
        (for [i (range 5 30)]
          [:option {:key (str "option_" i)
                    ;; :selected (if (= i @n) true false)
                    :value i}
           i])]]]
     [:div.control
      [:button
       {:type :button
        :class (concat
                ["button" "is-warning"]
                (if @(rf/subscribe [:current-game-generating?])
                 ["is-loading"]
                 []))
        :on-click generate-solution-and-game}
       "get game"]]]))

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

(def tadam (js/Audio. "media/tada.mp3"))

(defn play-view []
  (let [ids (rf/subscribe [:sudoku-ids])
        game (rf/subscribe [:current-game])
        solved? (rf/subscribe [:current-game-solved?])
        sound-triggered? (rf/subscribe [:current-game-sound-triggered?])]
    [:div.section.play-sudoku
     [select-var-count]
     (when (and @game @solved?)
       (when-not @sound-triggered?
         (.play tadam)
         (rf/dispatch [:set-current-trigger-sound]))
       [winning-modal])
     
     (when @game
       [game-component game input-control-fn])
     [:hr]]))
