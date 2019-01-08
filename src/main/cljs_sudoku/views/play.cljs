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

(defn generation-status [status label]
  (let [status-chan (async/timeout 1500)
        status-id (random-uuid)
        status-sub (rf/subscribe [:current-game-generation-status])]
    (when @status-sub
      (async/close! (:timeout @status-sub)))
    (rf/dispatch-sync [:set-game-generation-status {:id status-id
                                                    :status status
                                                    :label label
                                                    :timeout status-chan}])
    (async/go-loop [_ (<! status-chan)]
      (when (and @status-sub (= status-id (:id @status-sub)))
        (rf/dispatch [:set-game-generation-status nil])))))

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
            (do
              (>! sol-chan  {:status :ok
                             :sudoku (:data solution-result)
                             :game (:game game-result)})
              (generation-status :ok "OK!"))
            (do
              (>! sol-chan {:status :error}
                  :message "Failed to generate game.")
              (generation-status :error "Failed, Try Again"))))
        (do
          (>! sol-chan  {:status :error}
              :message "Failed to generate solution.")
          (generation-status :error "Failed")))
      (rf/dispatch [:set-game-is-generating false]))))

(defn get-game-button []
  (let [status (rf/subscribe [:current-game-generation-status])]
    [:button
     {:type :button
      :class (concat
              ["button" (cond
                          (nil? @status ) "is-primary"
                          (= :ok (:status @status)) "is-success"
                          (= :error (:status @status)) "is-danger")]
              (if @(rf/subscribe [:current-game-generating?])
                ["is-loading"]
                []))
      :on-click generate-solution-and-game}
     (if (nil? @status)
       "get game"
       (:label @status))]))

(defn select-var-count []
  (let [n (rf/subscribe [:current-game-var-count])
        fails (rf/subscribe [:current-game-generation-consecutive-failures])]
    [:div.field
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
        [get-game-button]]]
     (when (>= @fails 5)
       [:p.help.is-danger "Try with fewer empty cells"])]))

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
       [game-component input-control-fn])
     [:hr]]))
