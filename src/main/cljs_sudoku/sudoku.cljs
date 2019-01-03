(ns cljs-sudoku.sudoku
  (:require [clojure.string :as string :refer [join]]
            [clojure.set :as sets :refer [difference union intersection]]))

(defn uniques [ns]
  (= (count ns) (count (set ns))))

(defn columns
  ([sudoku] (columns [] sudoku))
  ([tmp sudoku]
   (if (<= (apply min (map count sudoku)) 0)
     tmp
     (let [heads (map first sudoku)]
       (recur (conj tmp (vec heads)) (map rest sudoku))))))

(defn uniques-rows-and-cols [sudoku]
  (and (every? uniques sudoku)
       (every? uniques (columns sudoku))))

(defn rand-lsquare
  ([]
   (let [s (shuffle (range 1 10))]
     (->> (partition 3 (vec s))
          (into []))))
  ([possibles]))

(defn rand-subpermutation [ns size]
  (->> (shuffle ns)
       (take size)
       (into [])))

(defn is-lsquare? [s]
  (and (= 3 (count s))
       (not-any? #(not= 3 %) (map count s))))

(defn set-board-pos [rows i j value]
  (-> rows
      (assoc-in [j i] value)))

(defprotocol BoardCoords
  (row [this i] "returns the i'th row")
  (col [this j] "returns the j'th column")
  (subboard [this i j] "returns the sub-board that has coord i, j as upper left corner")
  (insert-subboard [this board i j] "insert subboard"))
  ;; (get-position [this x y] "gets value at position x y")
  ;; (set-position [this x y value] "sets value"))

(defprotocol Positions
   (get-position [this x y] "gets value at position x y")
   (set-position [this x y value] "sets value"))

(defn subboard? [s]
  (and (vector? s)
       (= 9 (count s))
       (uniques s)
       (every? integer? s)
       (every? #(> % 0) s)))

(defn get-pos [rows i j]
  (get-in rows [j i]))

(defn possible-values [rows i j]
  (let [all-vals (set (range 1 10))
        row      (filter #(> % 0) (nth rows j))
        col      (filter #(> % 0) (nth (columns rows) i))]
    (difference all-vals row col)))

(defn valid-subboard-coord? [i j]
  (let [positions #{0 3 6}]
    (and (positions i) (positions j))))

(defrecord SubBoard [vals]
  BoardCoords
  (row [this i]
    (nth (:vals this) i))
  (col [this j]
    (->> (partition 3 (:vals this))
         (map #(nth % j))))
  (subboard [this i j]
    this)

  (insert-subboard [this board i j] board))

(defn build-subboard [vals] (->SubBoard vals))

(declare build-sudoku)


(defn get-position [this x y] (get-in this  [:rows y x]))
(defn set-position [this x y value]
              (-> (assoc-in this [:rows y x] value)
                  :rows
                  (build-sudoku)))

(defn insert-subboard
    [this board i j]
    (println "hola")
    (let [ps       (for [x (range i (+ 3 i))
                         y (range j (+ 3 j))]
                     [x y])
          new-rows (loop [positions ps
                          rows2     rows]
                     (if (empty? positions)
                       rows2
                       (let [[x1 y1] (first positions)
                             board-pos (+ (* (- x1 i) 3) (- y1 j))]
                         (recur (rest positions)
                                (set-board-pos rows2 y1 x1
                                               (nth board board-pos))))))]
      (build-sudoku new-rows)))

(defn row [this i]
  (nth (:rows this) i))

(defn col [this i]
  (nth (columns (:rows this)) i))

(defn subboard [this i j]
  (let [i1 (long (/ i 3))
        j1 (long (/ j 3))
        values (for [i2 (range 3)
                     j2 (range 3)
                     :let [i3 (+ i2 i1)
                           j3 (+ j2 j1)]]
                 (get-position this i3 j3))]
    (build-subboard (into [] values))))

(defrecord Sudoku
           [rows])
  
  

(defn build-sudoku [rows]
  (->Sudoku rows))

(defn ^Sudoku empty-sudoku []
  (->Sudoku
   (->> (repeat 9
                (->> (repeat 9 0)
                     (into [])))
        (into []))))

(defn  cell-neighbors [rows i j]
  ;; (println rows i j)
  (let [r    (into #{} (row rows j))
        c    (into #{} (col rows i))
        subx (long (/ i 3))
        suby (long (/ j 3))]
    (disj (union r c (into #{} (subboard rows i j))) 0)))

(defn available-values [^Sudoku rows i j]
  (let [neighbors (into #{} (cell-neighbors rows i j))
        all       (into #{} (range 1 10))]
    (difference all neighbors)))

(defn neighbors [pos x y]
  (filter #(or (= x (:x %)) (= y (:y %))) pos))

(defn new-point [i j] {:x i :y j})

(def emty-s (empty-sudoku))

(defn ^Sudoku insert-sb [sud sb i j]
  (let [ps (for [sb-x (range 3)
                 sb-y (range 3)
                 :let [sud-x (+ i sb-x)
                       sud-y (+ j sb-y)
                       sb-pos (+ (* 3 sb-y) sb-x)]]
             {:sb-pos sb-pos
              :sb-val (nth sb sb-pos)
              :x sud-x
              :y sud-y})]
    (reduce (fn [res p]
              (set-position
               res
               (:x p)
               (:y p)
               (:sb-val p))) sud ps)))

(def ^Sudoku diag-s
  (-> emty-s
      (insert-sb (rand-subpermutation (range 1 10) 9) 0 0)
      (insert-sb (rand-subpermutation (range 1 10) 9) 3 3)
      (insert-sb (rand-subpermutation (range 1 10) 9) 6 6)))

(defn sb-freedom [sud i j]
  (let [ps (for [sb-x (range 3)
                 sb-y (range 3)
                 :let [sud-x (+ i sb-x)
                       sud-y (+ j sb-y)
                       sb-pos (+ (* 3 sb-y) sb-x)]]
             {:sb-pos sb-pos
              :sb-val (available-values sud sud-x sud-y)
              :x sud-x
              :y sud-y})]
    ps))

(defn ^Sudoku clear-sb [sud i j]
  (-> sud
      (insert-sb (build-subboard (repeat 9 0)) i j)))

(defn range-covered? [freedom]
  (empty? (difference
           (into #{} (range 1 10))
           (into #{} (apply concat (map :sb-val freedom))))))

(defn enough-freedom? [freedom]
  (and
   (every? (comp not empty? :sb-val) freedom)
   (range-covered? freedom)))

(defn shuff-pos-val [pos-val]
  (loop [i (count pos-val)
         values pos-val]
    (if (= 0 i)
      values
      (let [[r1 r2] (split-at (rand-int (count values)) values)]
        (recur (dec i) (->> (concat r2 r1) (into [])))))))

(defn posible-sb [sud i j]
  (let [freedom (sb-freedom sud i j)
        pos-val (sort-by
                 :pos
                 (for [f freedom
                       v (:sb-val f)]
                   {:pos   (:sb-pos f)
                    :value v}))]
    (assert (enough-freedom? freedom))
    (loop [result []
           tries  0]
      (assert (< tries 10))
      (if (= 9 (count result))
        (->> result
             (sort-by :pos)
             (map :value)
             (into []))
        (let [new-result (reduce
                          (fn [res c]
                            (if (= 0
                                   (count
                                    (filter
                                     #(or (= (:value c) (:value %))
                                          (= (:pos c) (:pos %)))
                                     res)))
                              (concat res [c])
                              res))
                          (vector)
                          ;; pos-val
                          (shuffle pos-val))]
          (recur new-result (inc tries)))))))

(defn is-freedom-node [id]
  (fn [n]
    (= id (:sb-pos n))))

(defn freedom-vals [f]
  (:sb-val f))

(defn ^Sudoku sku []
  (let [s (empty-sudoku)
        initial (-> s
                    (insert-sb (rand-subpermutation (range 1 10) 9) 6 6))
        build-sb (fn [sud i j]
                   (insert-sb sud (posible-sb sud i j) i j))
        final   (-> initial
                    (build-sb 3 3)
                    (build-sb 6 3)
                    (build-sb 0 0)
                    (build-sb 0 3)
                    (build-sb 0 6)
                    (build-sb 3 6)
                    (build-sb 6 0)
                    (build-sb 3 0))]
    final))

(defn maybe-sku []
  (try
    {:result :ok
     :data (sku)}
    (catch js/Error _
      {:result :error})))

(defonce sudokus (atom {}))

(defn cache-sudoku [^Sudoku s]
  (let [id (random-uuid)
        created (js/Date.)]
    (->> {:id id
          :sudoku s
          :created created}
         (swap! sudokus assoc id))
    s))

(defn random-sudoku []
  (loop [retries 50
         res (maybe-sku)]
    (if (< retries 0)
      res
      (if (= :ok (:result res))
        (do
          (->> res
               (:data)
               (cache-sudoku))
          res)
        (recur (dec retries) (maybe-sku))))))

(defn print-sudoku [s]
  (assert (isa?  (type s) Sudoku))
  (doseq [r (doall (:rows s))
          :let [line (join "   " r)]]
    (println line)
    (println)))

(defn get-past-sudokus []
  @sudokus)


(defn neighbor-positions [i j]
  (let [sb-x (long (/ i 3))
        sb-y (long (/ j 3))]
    (-> #{}
         (into (for [a (range 9)] {:x i :y a}))
         (into (for [a (range 9)] {:x a :y j}))
         (into (for [a (range 3) b (range 3)] {:x (+ (* 3 sb-x) a) :y (+ (* 3 sb-y) b)})))))

(declare game-state)
(defrecord SudokuVar [x y value])

(defprotocol SudokuGameP
  (get-solution [this])
  (get-vars [this])
  (get-game-state [this]))

(defrecord SudokuGame [solution vars]
  SudokuGameP
  (get-solution [this] (:solution this))
  (get-vars [this] (:vars this))
  (get-game-state [this] (game-state this)))

(defn ^Sudoku game-state
  [^SudokuGame game]
  (reduce (fn [^Sudoku res v]
            (->Sudoku
             (assoc-in (:rows res) [(:y v) (:x v)] (:value v))))
          (:solution game)
          (:vars game)))

(defn valid-solution? [^Sudoku sudoku]
  (let [cells (for [i (range 9)
                    j (range 9)]
                {:x i
                 :y j
                 :value (get-position sudoku i j)})
        all-defined? (every? #(not= 0 %) (map :value cells))
        freedom (map #(available-values sudoku (:x %) (:y %)) cells)
        no-freedom? (every? #(= 0 (count %)) freedom)]
    (and all-defined? no-freedom?)))

(defn is-single-solution-game? [^SudokuGame game]
  (let [^Sudoku state (get-game-state game)
        var-freedom (for [v (filter #(not= 0 %) (:vars game))]
                      (available-values state (:x v) (:y v)))]
    (every? #(= 1 (count %)) var-freedom)))


(defn random-sudoku-positions [n]
  (let [all-positions (for [i (range 9)
                            j (range 9)]
                        {:x i :y j})
        shuffled (shuffle all-positions)]
    (take n shuffled)))

(defn get-block-number [try-number]
  (long (mod try-number 9)))

(defn get-block-base [try-number]
  (let [block-number (get-block-number try-number)]
    {:x (* 3 (mod block-number 3))
     :y (* 3 (long (/ block-number 3)))}))

(defn positions-by-block []
  (into {} (for [i [0 4 8 1 7 3 5 2 6]]
             [i (->>
                 (for [x (range 3) y (range 3)
                       :let [cell (-> (get-block-base i)
                                     (update :x + x)
                                     (update :y + y))]]
                   cell)
                 (into [])
                 (shuffle))])))

(defn random-positions-2 [n]
  (let [positions (positions-by-block)]
    (loop [remaining n
           selected []]
      (if (>= 0 remaining)
        selected
        (recur
         (dec remaining)
         (into selected [(get-in positions
                                 [(mod remaining 6)
                                  (long (/ (count selected) 6))])]))))))

(defn ^SudokuGame random-game [^Sudoku solution var-count]
  (let [positions (random-sudoku-positions var-count)
        vars (map merge positions (repeat {:value 0}))]
    (->SudokuGame solution vars)))

(defn generate-game [solution var-count]
  (loop [retries 2000
         game (random-game solution var-count)]
    (cond
      (= retries 0) {:status :error
                     :message "Failed to find a valid game"}
      (is-single-solution-game? game) {:status :ok
                                       :game game}
      :else (recur (dec retries) (random-game solution var-count)))))
