(ns cljs-sudoku.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-sudoku.sudoku :as s]))

(rf/reg-event-db
 :initialize
 (fn-traced [db _]
            (-> {}
                (assoc :current-sudoku nil)
                (assoc :current-view :play)
                (assoc :loading? false)
                (assoc :animating? false)
                (assoc :sudoku-cache {})
                (assoc :navbar {:items {:history {:id :history
                                                  :type :view
                                                  :label "view previous solutions"}
                                        :regular {:id :regular
                                                  :type :view
                                                  :label "generate solutions"}
                                        :play {:id :play
                                               :type :view
                                               :label "play sudoku"}
                                        :github {:id :github
                                                 :type :link
                                                 :label "view on github"
                                                 :url "http://github.com/baskeboler/cljs-sudoku"}}
                                :active? false})
                (assoc :views {:regular {}
                               :past-sudokus {:current 0
                                              :start 0
                                              :end 0
                                              :sudoku nil}
                               :play {:current 0
                                      :current-game nil
                                      :var-count 15
                                      :generating? false
                                      :status nil
                                      :consecutive-generation-failures 0}}))))
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

(rf/reg-event-fx
 :set-current-view
 (fn-traced [{:keys [db]} [_ view]]
            {:db (-> db
                     (assoc :current-view view))
             :dispatch [:toggle-navbar-menu]}))

(rf/reg-event-db
 :set-current-game
 (fn-traced [db [_ game]]
      (-> db
          (assoc-in [:views :play :current-game] game)
          (assoc-in [:views :play :sound-triggered?] false))))

(rf/reg-event-fx
 :set-current-sudoku-and-game
 (fn-traced [{:keys [db]} [_ sud id created game]]
            {:db db
             :dispatch-n [[:set-current-sudoku sud id created]
                          [:set-current-game game]]}))

(rf/reg-event-db
 :set-current-trigger-sound
 (fn-traced [db _]
            (assoc-in db [:views :play :sound-triggered?] true)))

(rf/reg-event-db
 :set-game-var-count
 (fn-traced [db [_ n]]
            (assoc-in db [:views :play :var-count] n)))

(rf/reg-event-db
 :set-game-var-value
 (fn-traced [db [_ x y value]]
            (update-in db [:views :play :current-game :vars]
                       (fn [vars]
                         (map (fn [v]
                                (if (and (= x (:x v)) (= y (:y v)))
                                  (assoc v :value value)
                                  v)) vars)))))
(rf/reg-event-db
 :set-game-is-generating
 (fn-traced [db [_ generating?]]
            (assoc-in db [:views :play :generating?] generating?)))

(rf/reg-event-fx
 :set-game-generation-status
 (fn-traced [{:keys [db]} [_ status]]
            {:db (assoc-in db [:views :play :status] status)
             :dispatch-n [(cond
                            (and status (= :ok (:status status)))
                            [:reset-game-generation-failures]
                            (and status (= :error (:status status)))
                            [:increment-game-generation-failures]
                            :else nil)]}))

(rf/reg-event-db
 :increment-game-generation-failures
 (fn-traced [db _]
            (update-in db [:views :play :consecutive-generation-failures] inc)))

(rf/reg-event-db
 :reset-game-generation-failures
 (fn-traced [db _]
            (assoc-in db [:views :play :consecutive-generation-failures] 0)))

(rf/reg-event-db
 :toggle-navbar-menu
 (fn-traced [db _]
            (update-in db [:navbar :active?] not)))

(rf/reg-event-fx
 :fetch-websudoku
 (fn-traced [{:keys [db]} _]
            {:db db
             :http-xhrio {:method :get
                          :uri "http://localhost:8080/sudoku"
                          :timeout 8000
                          :response-format (ajax.core/json-response-format {:keywords? true})
                          :on-success [:fetch-websudoku-success]
                          :on-failure [:fetch-websudoku-failure]}}))

(rf/reg-event-db
 :fetch-websudoku-success
 (fn-traced [db [_ result]]
            (-> db
                (assoc :websudoku-response result)
                (assoc :websudoku-solution (s/->Sudoku (:solution result))))))
