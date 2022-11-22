(ns cljs-sudoku.components
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.set :refer [union difference]]
            [cljs-sudoku.sudoku :as s]
            [cljs.core.async :as async :refer [<! >! go]]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy]))

(def grid-style
  {:display "flex"
   :flex-flow "row wrap"
  ;;  :width "100%"
   :margin "2em"
   :min-width "240px"})

(def tile-style
  {:flex-basis "calc(100%/9)"
   :border "1px solid black"
   :padding 0
   ::stylefy/mode
     {:after {:content ""
              :padding-bottom "100%"
              :display "block"}
      ":nth-child(3n):not(:nth-child(9n))"
      {:border-right "5px solid black"}
      ":nth-child(n+19):nth-child(-n+27)"
      {:border-bottom "5px solid black"}
      ":nth-child(n+46):nth-child(-n+54)"
      {:border-bottom "5px black solid"}}
   ::stylefy/sub-styles {:content {:position "relative"
                                   :text-align :center
                                   :left 0
                                   :right 0}}})
(defn sudoku-component [sud]
  (let [highlighted (atom #{})
        highlighted-main (atom #{})
        highlighted-secondary (atom #{})
        highlighted? (atom false)
        cell-click-fn (fn [x y value]
                        (fn []
                          (when-not @highlighted?
                            (reset! highlighted? true)
                            (swap! highlighted union (s/neighbor-positions x y))
                            (swap! highlighted-secondary conj value)
                            (swap! highlighted-main conj {:x x :y y})
                            (go
                              (<! (async/timeout 750))
                              (swap! highlighted difference (s/neighbor-positions x y))
                              (swap! highlighted-main disj {:x x :y y})
                              (swap! highlighted-secondary disj value)
                              (reset! highlighted? false)))))] 
    (fn []
      (if (and sud @sud)
        [:div.m-5 (stylefy/use-style grid-style)
         (doall
          (for [[i r] (map-indexed #(vector %1 %2) (:rows @sud))]
            (doall
             (for [[j n] (map-indexed #(vector %1 %2) r)]
               [:div (stylefy/use-style tile-style
                                        {:key (str "cell_" i "_" j)
                                         :class "cell"})
                 [:div (stylefy/use-sub-style
                           tile-style
                           :content
                           {:class [(when (@highlighted-main {:x j :y i})
                                      "highlighted-main")
                                    (when (@highlighted {:x j :y i})
                                      "highlighted")
                                    (when (@highlighted-secondary n)
                                      "highlighted-secondary")]
                             :on-click (cell-click-fn j i n)})
                  n]]))))]
        [:div "Click on \"generate\"."]))))

(def input-style
  {:font-size "inherit"
   :text-align :center
   :padding 0
   :margin 0
   :border :none
   :color :grey
   :height "100%"
   :width "100%"})

(defn game-component [input-fn]
  (let [highlighted (atom #{})
        highlighted-main (atom #{})
        highlighted-secondary (atom #{})
        highlighted? (atom false)
        game (rf/subscribe [:current-game])
        cell-click-fn (fn [x y value is-var?]
                        (fn []
                          (when-not (or is-var? @highlighted?)
                            (reset! highlighted? true)
                            (swap! highlighted union (s/neighbor-positions x y))
                            (swap! highlighted-secondary conj value)
                            (swap! highlighted-main conj {:x x :y y})
                            (go
                              (<! (async/timeout 750))
                              (swap! highlighted difference (s/neighbor-positions x y))
                              (swap! highlighted-main disj {:x x :y y})
                              (swap! highlighted-secondary disj value)
                              (reset! highlighted? false)))))
        change-fn (fn [x y]
                    (fn [evt]
                      (js/console.log (-> evt .-target))
                      (let [new-number (js/Number.parseInt (-> evt .-target .-value))]
                        (input-fn x y new-number))))]
    (fn []
      (if (and game @game)
        [:div (stylefy/use-style grid-style)
         (doall
          (for [[j r] (map-indexed #(vector %1 %2) (get-in @game [:solution :rows]))]
            (doall
             (for [[i n] (map-indexed #(vector %1 %2) r)
                   :let [var-set (->> (get @game :vars)
                                      (map #(dissoc % :value))
                                      (into #{}))
                         is-var? (var-set {:x i :y j})]]
               [:div (stylefy/use-style tile-style
                                        {:key (str "cell_" i "_" j)
                                         :class "cell"})
                 [:div (stylefy/use-sub-style
                           tile-style
                           :content
                           {:class (if-not is-var?
                                     [(when (@highlighted-main {:x i :y j})
                                        "highlighted-main")
                                      (when (@highlighted {:x i :y j})
                                        "highlighted")
                                      (when (@highlighted-secondary n)
                                        "highlighted-secondary")]
                                     [])
                             :on-click (cell-click-fn i j n is-var?)})
                  (if is-var?
                    [:input (stylefy/use-style
                             input-style
                             {:type :number
                              :on-change (change-fn i j)
                              :value (let [value @(rf/subscribe [:current-game-var-value i j])]
                                       (if (not= 0 value)
                                         value
                                         ""))})]
                    n)]]))))]
        [:div "Click on \"generate\"."]))))


(defn pagination
  [start end current fn-page]
  (let [paging-item (fn [page-num]
                      [:li>a
                       {:class ["pagination-link" (when (= @current page-num) "is-current")]
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
                                 ^{:key (str "item_" i)}
                                 [paging-item i]))
       (> 3 (- @current @start)) (doall
                                  (concat
                                   (for [i (range @start (+ 2 @current))
                                         :when (> @end i)]
                                     ^{:key (str "item_" i)}
                                     [paging-item i])
                                   [[:li {:key "elipsis_1"}
                                     [:span.pagination-ellipsis "..."]]
                                    ^{:key (str "item_" @end)}
                                    [paging-item (dec @end)]]))
       (> 3 (- @end @current)) (doall
                                (concat
                                 [^{:key "item_0"}
                                   [paging-item @start]
                                  [:li>span.ellipsis {:key "elipsis_1"} "..."]]
                                 (for [i (range (- @current 1) @end)
                                       :when (> @end i)]
                                   ^{:key (str "item_" i)}
                                   [paging-item i])))
       :else (doall
              (concat
               [^{:key "item_0"}
                [paging-item @start]
                [:li>span.pagination-ellipsis {:key "elipsis_1"} "..."]]
               (for [i (range (- @current 1) (+ @current 2))
                     :when (> @end i)]
                 ^{:key (str "item_" i)}
                 [paging-item i])
               [[:li>span.pagination-ellipsis {:key "elipsis_2"} "..."]
                ^{:key (str "item_" @end)}
                [paging-item  (dec @end)]])))]]))

(defmulti nav-item :type)

(defmulti nav-item-mobile :type)
(defmethod nav-item :view [v]
  [:a
   {:on-click #(rf/dispatch [:set-current-view (:id v)]),
    :class
    (str "rounded-md px-3 py-2 text-sm font-medium "
         (if (= (:id v) @(rf/subscribe [:current-view]))
           "bg-gray-900 text-white"
           "text-gray-300 hover:bg-gray-700 hover:text-white"))}
   (:label v)]
  #_[:a.navbar-item {:on-click #(rf/dispatch [:set-current-view (:id v)])}
   (:label v)])

(defmethod nav-item-mobile :view [v]
  [:a
   {:on-click #(rf/dispatch [:set-current-view (:id v)]),
    :class
    (str "block rounded-md px-3 py-2 text-base font-medium  "
         (if (= (:id v) @(rf/subscribe [:current-view]))
           "bg-gray-900 text-white"
           "text-gray-300 hover:bg-gray-700 hover:text-white"))}
   (:label v)]
  #_[:a.navbar-item {:on-click #(rf/dispatch [:set-current-view (:id v)])}
     (:label v)])

(defmethod nav-item :link [l]
   [:a
    {:href (:url l) ,
     :class
     "rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}
    (:label l)] )

(defmethod nav-item-mobile :link [l]
   [:a
    {:href (:url l) ,
     :class
     "block rounded-md px-3 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}
    (:label l)]
  #_[:a.navbar-item {:href (:url l)
                   :target :_blank}
   (:label l)])

(defmethod nav-item :tw-desktop [l])



(defn tw-navbar []
  [:nav
   {:class "bg-gray-800"}
   [:div
    {:class "mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"}
    [:div
     {:class "flex h-16 items-center justify-between"}
     [:div
      {:class "flex items-center"}
      [:div
       {:class "flex-shrink-0"}
       [:img
        {:class "block h-8 w-auto lg:hidden",
         :src
         "https://tailwindui.com/img/logos/mark.svg?color=indigo&shade=500",
         :alt "Your Company"}]
       [:img
        {:class "hidden h-8 w-auto lg:block",
         :src
         "https://tailwindui.com/img/logos/mark.svg?color=indigo&shade=500",
         :alt "Your Company"}]]
      [:div
       {:class "hidden sm:ml-6 sm:block"}
       [:div
        {:class "flex space-x-4"}
        (for [[item data] @(rf/subscribe [:navbar-items])]
          (with-meta [nav-item data] {:key (str "item_" item)}))]]]
     [:div
      {:class "hidden sm:ml-6 sm:block"}
      [:div
       {:class "flex items-center"}]]
     [:div
      {:class "-mr-2 flex sm:hidden"}
      [:button
       {:type "button",
        :on-click #(rf/dispatch [:toggle-navbar-menu])
        :class
        "inline-flex items-center justify-center rounded-md p-2 text-gray-400 hover:bg-gray-700 hover:text-white focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white",
        :aria-controls "mobile-menu",
        :aria-expanded "false"}
       [:span {:class "sr-only"} "Open main menu"]
       [:svg
        {:class "block h-6 w-6",
         :xmlns "http://www.w3.org/2000/svg",
         :fill "none",
         :viewBox "0 0 24 24",
         :stroke-width "1.5",
         :stroke "currentColor",
         :aria-hidden "true"}
        [:path
         {:stroke-linecap "round",
          :stroke-linejoin "round",
          :d "M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"}]]
       [:svg
        {:class "hidden h-6 w-6",
         :xmlns "http://www.w3.org/2000/svg",
         :fill "none",
         :viewBox "0 0 24 24",
         :stroke-width "1.5",
         :stroke "currentColor",
         :aria-hidden "true"}
        [:path
         {:stroke-linecap "round",
          :stroke-linejoin "round",
          :d "M6 18L18 6M6 6l12 12"}]]]]]]
   [:div
    {:class (str "sm:hidden " (when-not @(rf/subscribe [:navbar-menu-active?]) "hidden")) , :id "mobile-menu"}
    [:div
     {:class "space-y-1 px-2 pt-2 pb-3"}
     (comment [:a
               {:href "#",
                :class
                "block rounded-md bg-gray-900 px-3 py-2 text-base font-medium text-white"}
               "Dashboard"]
              
              [:a
               {:href "#",
                :class
                "block rounded-md px-3 py-2 text-base font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}
               "Team"]
              
              [:a
               {:href "#",
                :class
                "block rounded-md px-3 py-2 text-base font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}
               "Projects"]
              
              [:a
               {:href "#",
                :class
                "block rounded-md px-3 py-2 text-base font-medium text-gray-300 hover:bg-gray-700 hover:text-white"}
               "Calendar"]
              )
     (for [[item data] @(rf/subscribe [:navbar-items])]
       (with-meta [nav-item-mobile data] {:key (str "item_" item)}))]
    #_[:div
     {:class "border-t border-gray-700 pt-4 pb-3"}
     [:div
      {:class "flex items-center px-5"}
      [:div
       {:class "flex-shrink-0"}
       [:img
        {:class "h-10 w-10 rounded-full",
         :src
         "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80",
         :alt ""}]]
      [:div
       {:class "ml-3"}
       [:div {:class "text-base font-medium text-white"} "Tom Cook"]
       [:div
        {:class "text-sm font-medium text-gray-400"}
        "tom@example.com"]]
      [:button
       {:type "button",
        :class
        "ml-auto flex-shrink-0 rounded-full bg-gray-800 p-1 text-gray-400 hover:text-white focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-gray-800"}
       [:span {:class "sr-only"} "View notifications"]
       [:svg
        {:class "h-6 w-6",
         :xmlns "http://www.w3.org/2000/svg",
         :fill "none",
         :viewBox "0 0 24 24",
         :stroke-width "1.5",
         :stroke "currentColor",
         :aria-hidden "true"}
        [:path
         {:stroke-linecap "round",
          :stroke-linejoin "round",
          :d
          "M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"}]]]]
     [:div
      {:class "mt-3 space-y-1 px-2"}
      [:a
       {:href "#",
        :class
        "block rounded-md px-3 py-2 text-base font-medium text-gray-400 hover:bg-gray-700 hover:text-white"}
       "Your Profile"]
      [:a
       {:href "#",
        :class
        "block rounded-md px-3 py-2 text-base font-medium text-gray-400 hover:bg-gray-700 hover:text-white"}
       "Settings"]
      [:a
       {:href "#",
        :class
        "block rounded-md px-3 py-2 text-base font-medium text-gray-400 hover:bg-gray-700 hover:text-white"}
       "Sign out"]]]]])