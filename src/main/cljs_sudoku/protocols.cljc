(ns cljs-sudoku.protocols)


(defprotocol BoardCoords
  (row [this i] "returns the i'th row")
  (col [this j] "returns the j'th column")
  (subboard [this i j] "returns the sub-board that has coord i, j as upper left corner")
  (insert-subboard [this board i j] "insert subboard"))

;; (defprotocol Positions
;;   (get-position [this x y] "gets value at position x y")
;;   (set-position [this x y value] "sets value"))

(defprotocol SudokuGameP
  (get-solution [this])
  (get-vars [this])
  (get-game-state [this]))

