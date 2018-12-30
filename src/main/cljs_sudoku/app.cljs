(ns cljs-sudoku.app
  (:require ["react"]
            [cljs-sudoku.events]
            [reagent.core :as reagent :refer [atom render]]
            [re-frame.core :as rf :include-macros true]
           
            [clojure.set :as sets :refer [difference union]]
            [cljs-sudoku.sudoku :as s :refer [random-sudoku neighbor-positions]]
            [cljs.core.async :as async :refer [<! >! chan put! go go-loop]
             :include-macros true]))
;; (rf/dispatch-sync [:initialize])
(rf/reg-sub
 :current-view
 (fn [db _] (:current-view db)))


(rf/reg-sub
 :current-sudoku
 (fn [db _] (:current-sudoku db)))

(rf/reg-sub
 :loading?
 (fn [db _] (:loading? db)))

(rf/reg-sub
 :animating?
 (fn [db _] (:animating? db)))

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

;; (def current-view (rf/subscribe [:current-view])))

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
                                   :dt (+ (*  90 j) (* i 100) 100)
                                   :new-val (s/get-position new-sudoku i j)}))
          finish-timeout (async/timeout 1000)]
      (rf/dispatch-sync [:set-animating? true])
      (doseq [change cell-changes]
        (let [to-chan (async/timeout (:dt change))]
              
          (go
            (<! to-chan)
            (rf/dispatch [:update-current-sudoku
                          (-> @(rf/subscribe [:current-sudoku])
                              (s/set-position
                               (:x change)
                               (:y change)
                               (:new-val change)))]))))
      (go
        (<! finish-timeout)
        (rf/dispatch [:set-animating? false])
        (rf/dispatch [:cache-sudoku new-sudoku (random-uuid) (js/Date.)])))))
     


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

(defn pagination
  [start end current fn-page]
  (let [paging-item (fn [page-num]
                      [:li>a
                       {
                        :class ["pagination-link" (when (= @current page-num) "is-current")]
                        :on-click #(fn-page page-num)}
                       page-num])]
   [:nav.pagination
    {:role :pagination
     :aria-label :pagination}
    [:a.pagination-previous {:on-click #(if (> @current @start) (fn-page (dec @current)))}
     "previous"]
    [:a.pagination-next {:on-click #(if (< @current @end) (fn-page (inc @current)))}
     "next"]
    [:ul.pagination-list
     (cond
       (> 10 (- @end @start)) (doall
                               (for [i (range @start @end)]
                                 [paging-item i {:key (str "item_" i)}]))
       (> 3 (- @current @start)) (doall
                                  (concat
                                   (for [i (range @start (+ 2 @current))
                                         :when (> @end i)]
                                     [paging-item i {:key (str "item_" i)}])
                                   [[:li
                                     [:span.pagination-ellipsis "..."]]
                                    [paging-item (dec @end)]]))
       (> 3 (- @end @current)) (doall
                                (concat
                                 [[paging-item @start]
                                  [:li>span.ellipsis "..."]]
                                 (for [i (range (- @current 1) @end)
                                       :when (> @end i)]
                                   [paging-item i {:key (str "item_" i)}])))
       :else (doall
              (concat
               [[paging-item @start]
                [:li>span.pagination-ellipsis "..."]]
               (for [i (range (- @current 1) (+ @current 2))
                     :when (> @end i)]
                 [paging-item i {:key (str "item_" i)}])
               [[:li>span.pagination-ellipsis "..."]
                [paging-item (dec @end)]])))]]))

(defn sudoku-component [sud]
  (let [highlighted (atom #{})
        highlighted-secondary (atom #{})
        cell-click-fn (fn [x y value]
                        (fn []
                          (swap! highlighted union (s/neighbor-positions x y))
                          (swap! highlighted-secondary conj value)
                          (go
                            (<! (async/timeout 2000))
                            (swap! highlighted difference (s/neighbor-positions x y))
                            (swap! highlighted-secondary disj value))))] 
    (fn []
      (if (and sud @sud)
        [:div.sudoku.card
         (doall
          (for [[i r] (map-indexed #(vector %1 %2) (:rows @sud))]
            [:div.columns.is-mobile.is-gapless.is-centered
             {:key (str "row_" i)}
             [:div.column
               (doall
                (for [[j n] (map-indexed #(vector %1 %2) r)]
                 [:span.cell
                  {:key (str "cell_" i "_" j)
                   :class ["cell"
                           (when (@highlighted {:x j :y i})
                             "highlighted")
                           (when (@highlighted-secondary n)
                             "highlighted-secondary")]
                   :on-click (cell-click-fn j i n)}
                  n]))]]))]
        [:div "Apreta el boton"]))))

(defn generate-btn []
      [:a
       {:on-click (generate-new)
        :class ["button" "is-large" "is-primary"
                "is-fullwidth" (when @(rf/subscribe [:loading?]) "is-loading")]}
       "quiero mas!"])

(defn navbar []
  [:nav.navbar.is-primary
   {:role :navigation}
   [:div.navbar-brand
    [:div.navbar-item
     [:h1
      "sudoku"]]
    [:div.navbar-item>a
     {:on-click #(if (= @(rf/subscribe [:current-view]) :regular)
                   (rf/dispatch [:set-current-view :history])
                   (rf/dispatch [:set-current-view :regular]))}
     (if (= @(rf/subscribe [:current-view]) :regular)
       "ver anteriores"
       "generá más")]]])

(defn past-sudokus-view []
  (let [view-data (rf/subscribe [:past-sudokus-view])
        sudoku-vec (->> (vals @s/sudokus)
                        (sort-by :created)
                        (map :sudoku)
                        (into []))
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
      "Generar"]
     [:hr]
     [sudoku-component (rf/subscribe [:current-sudoku])]]])
(defn app []
  [:div.app
   [navbar]
   (cond
     (= @(rf/subscribe [:current-view])
        :regular) [regular-view]
     (= @(rf/subscribe [:current-view])
        :history) [past-sudokus-view])])

(defn mount-components! []
 (render
   [app]
   (.getElementById js/document "root")))

(defn init! []
  (rf/dispatch-sync [:initialize])
  (rf/dispatch [:init-past-sudokus-view])
  (let [sudoku   (rf/subscribe [:current-sudoku])
        loading? (rf/subscribe [:loading?])
        animating? (rf/subscribe [:animating?])]
    (mount-components!)))

;; (do
  ;; (init!)) 
