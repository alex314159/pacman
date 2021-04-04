(ns pacman.core
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]))


(def board-size-x 20)
(def board-size-y 20)
(def quil-size-x 800)
(def quil-size-y 800)
(def nb-of-walls 150)
(def nb-of-ghosts 10)
(def frames-per-seconds 30)
(def ghost-speed-in-frames 10)
(def qxsq (/ quil-size-x board-size-x))
(def qysq (/ quil-size-y board-size-y))


;;;;;;;;;;;;;;;;;;;;;;
;; TABLE DEFINITION ;;
;;;;;;;;;;;;;;;;;;;;;;

; We use a vector of vector that contains the following keys:
; :p pac-man
; :f food
; :i exit
; :w wall
; :g ghost on empty cell
; :h ghost on food cell

(def food-table (vec (repeat board-size-x (vec (repeat board-size-y :f)))))


(defn change-vector
  [v e target]
  (assoc v e target))


(defn change-table
  "Simplest possible version"
  [table x y target]
  (let [new-col (change-vector (table x) y target)]
    (change-vector table x new-col)))

;(defn change-table-2
;  "Cleaner but more confusing?"
;  [table x y target]
;  (update table x change-vector y target))


(defn get-value
  "Unnecessary but feels cleaner"
  [table x y]
  ((table x) y))


(defn add-random-element
  "Adds a random wall to the table if the random x y is food, otherwise returns original table
  Avoids first and last corner not to block pac-man and the exit"
  [table element]
  (let [x (rand-int (dec board-size-x))
        y (rand-int (dec board-size-y))]
    (cond
      (and (<= (+ x y) 2)) table                            ;top left
      (and (>= (+ x y) (+ board-size-x board-size-y -4))) table ;bottom right
      (= (get-value table x y) :f) (change-table table x y element) ;checks it's only overriding food
      :else table)))                                        ;could be anything else


(defn fill-table-with-n-walls-and-m-ghosts
  "From the empty table, we add :p at top left, :i at bottom right, and up to n random walls and m ghosts"
  [n m]
  (let [table-with-ghosts (last (take (inc m) (iterate #(add-random-element % :h) food-table)))
        table-with-walls (last (take (inc n) (iterate #(add-random-element % :w) table-with-ghosts)))]
    (-> table-with-walls
        (change-table 0 0 :p)
        (change-table (dec board-size-x) (dec board-size-y) :i))))


(defn new-position
  "From existing x y we return the new x y after keypress, or the same x y if the key isn't up/down/left/right"
  [x y keypress]
  (case keypress
    :up         [x (dec y)]
    :ArrowUp    [x (dec y)]
    :down       [x (inc y)]
    :ArrowDown  [x (inc y)]
    :left       [(dec x) y]
    :ArrowLeft  [(dec x) y]
    :right      [(inc x) y]
    :ArrowRight [(inc x) y]
    [x y]))


(defn find-element-in-column
  [col pred]
  (mapv first
        (filter #(pred (second %))
                (map-indexed vector col))))


(defn find-element-in-table
  "We flatten the table, look for the indices, then translate back to coordinates."
  [table element]
  (let [flat-results (find-element-in-column (apply concat table) #(= % element))]
    (into [] (for [x flat-results] [(quot x board-size-y) (rem x board-size-y)]))))

;(defn find-element-in-table-alternative
;  [table element]
;  (let [y-search (mapv (fn [col] (find-element-in-column col #(= % element))) table)
;        x-search (find-element-in-column y-search seq)]
;    (into [] (for [x x-search y (y-search x)] [x y]))))

;Easier case where we know there's just one element:
;(defn find-pacman-in-column
;  "We look for the position x of pacman in a vector. If not found will return -1."
;  [v]
;  (.indexOf v :p))
;
;(defn find-pacman-in-table
;  "We look for the position x y of pacman in the table"
;  [table]
;  (let [res (mapv find-pacman-in-column table)
;        x (.indexOf (map neg? res) false)]
;    [x (res x)]))


(defn table-after-actor-move [table actor x y keypress]
  (let [[xnew ynew] (new-position x y keypress)]
    (cond
      (< xnew 0)                table ;"Can't break outside wall!"
      (>= xnew board-size-x)    table ;"Can't break outside wall!"
      (< ynew 0)                table ;"Can't break outside wall!"
      (>= ynew board-size-y)    table ;"Can't break outside wall!"
      (= (get-value table xnew ynew) :w) table ;"Can't break inside wall!"
      (and (= actor :p) (= (get-value table xnew ynew) :g)) (-> table (change-table x y :e) (change-table xnew ynew :g)) ;Pac-man hit a ghost!
      (and (= actor :p) (= (get-value table xnew ynew) :h)) (-> table (change-table x y :e) (change-table xnew ynew :h)) ;Pac-man hit a ghost!
      (and (= actor :p) (= (get-value table xnew ynew) :i)) (-> table (change-table x y :e) (change-table xnew ynew :p)) ;Pac-man got to the exit!
      (= actor :p)      (-> table (change-table x y :e) (change-table xnew ynew :p)) ;Pac-man is moving
      (and (= actor :g) (= (get-value table xnew ynew) :e)) (-> table (change-table x y :e) (change-table xnew ynew :g)) ;Ghost moved
      (and (= actor :g) (= (get-value table xnew ynew) :f)) (-> table (change-table x y :e) (change-table xnew ynew :h)) ;Ghost moved
      (and (= actor :g) (= (get-value table xnew ynew) :p)) (-> table (change-table x y :e) (change-table xnew ynew :g)) ;Ghost hit Pac-man!
      (and (= actor :h) (= (get-value table xnew ynew) :e)) (-> table (change-table x y :f) (change-table xnew ynew :g)) ;Ghost moved
      (and (= actor :h) (= (get-value table xnew ynew) :f)) (-> table (change-table x y :f) (change-table xnew ynew :h)) ;Ghost moved
      (and (= actor :h) (= (get-value table xnew ynew) :p)) (-> table (change-table x y :f) (change-table xnew ynew :g)) ;Ghost hit Pac-man!
      :else table)));do nothing


(defn table-after-pac-man-move
  [table keypress]
  (let [[x y] (first (find-element-in-table table :p))]
    (table-after-actor-move table :p x y keypress)))


(defn table-after-ghost-move
  "We simulate a random key-press for each ghost"
  [table x y]
  (let [ghost-keypress (rand-nth [:up :down :left :right])]
    (table-after-actor-move table (get-value table x y) x y ghost-keypress)))


(defn move-all-ghosts
  [table]
  (let [ghost-positions (mapcat #(find-element-in-table table %) [:h :g])]
    (reduce #(table-after-ghost-move %1 (first %2) (second %2)) table ghost-positions)))


(defn count-element
  "We count how many times element is inside the table"
  [table element]
  (count
    (filter #(= % element) (apply concat table))))


(defn nice-table-display
  "This transposes the vector of vectors so it looks like what quil will draw. Only useful for REPL debugging."
  [table]
  (apply map list table))


;;;;;;QUIL DRAWING STARTS HERE;;;;;;;

(defn starting-state []
  {:table (fill-table-with-n-walls-and-m-ghosts nb-of-walls nb-of-ghosts)
   :end   false
   :frame 0})

(defn setup []
  (q/frame-rate frames-per-seconds)
  (starting-state))

(defn update-state
  "At each frame, we either return the same table or move the ghosts if it's time.
  We then check if the game is finished by checking if both the exit and Pac-man are still there."
  [state]
  (let [f (:frame state)]
    {:table (if (and (not (:end state)) (zero? (rem f ghost-speed-in-frames))) (move-all-ghosts (:table state)) (:table state))
     :end   (or (zero? (count-element (:table state) :i)) (zero? (count-element (:table state) :p)))
     :frame (inc f)}))

(defn status-box
  [table line]
  (q/no-stroke)
  (q/fill "white")
  (q/rect (+ 10 quil-size-x) 75 180 70)
  (q/fill "black")
  (q/text line (+ 20 quil-size-x) 95)
  (q/text (str "Score: " (+ (count-element table :e) (count-element table :g))) (+ 20 quil-size-x) 120))

(defn on-key-down
  "Restart when r is pressed, otherwise if the game is still going move Pac-man with arrows up/down/left/right.
  Checks if Pac-man won after moving."
  [state event]
  (cond
    (= :r (:key event)) (starting-state)
    (not (:end state)) (let [new-table (table-after-pac-man-move (:table state) (:key event))]
                         (assoc state :table new-table :end (zero? (count-element new-table :i))))
    :else state))

(defn on-key-up [state] state)

(defn draw-pacman
  [x y]
  (q/stroke "black")
  (q/fill "black")
  (q/rect (* qxsq x) (* qysq y) qxsq qysq)
  (q/fill "yellow")
  (q/ellipse (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.75 qxsq) (* 0.75 qysq))
  (q/fill "black")
  (q/arc (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.75 qxsq) (* 0.75 qysq) (- q/QUARTER-PI) q/QUARTER-PI))

(defn draw-ghost [x y on-food?]
  (q/fill "black")
  (q/rect (* qxsq x) (* qysq y) qxsq qysq)
  ;(when on-food?
  ;  (q/fill "green")
  ;  (q/ellipse (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.25 qxsq) (* 0.25 qysq)))
  (q/stroke "blue")
  (q/stroke-weight 3)
  (q/arc (+ (* qxsq 0.5) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.75 qxsq) (* 0.75 qysq) (* -4 q/QUARTER-PI) (* 0 q/QUARTER-PI))
  (q/fill "blue")
  (q/rect (+ (* qxsq 0.125) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* qxsq 0.75) (* 0.4 qysq))
  (q/stroke "black")
  (q/fill "black")
  (q/triangle (+ (* qxsq 0.125) (* qxsq x)) (+ (* qysq 0.9) (* qysq y))
              (+ (* qxsq 0.375) (* qxsq x)) (+ (* qysq 0.9) (* qysq y))
              (+ (* qxsq 0.25) (* qxsq x)) (+ (* qysq 0.75) (* qysq y)))
  (q/triangle (+ (* qxsq 0.375) (* qxsq x)) (+ (* qysq 0.9) (* qysq y))
              (+ (* qxsq 0.625) (* qxsq x)) (+ (* qysq 0.9) (* qysq y))
              (+ (* qxsq 0.5) (* qxsq x)) (+ (* qysq 0.75) (* qysq y)))
  (q/triangle (+ (* qxsq 0.625) (* qxsq x)) (+ (* qysq 0.9) (* qysq y))
              (+ (* qxsq 0.875) (* qxsq x)) (+ (* qysq 0.9) (* qysq y))
              (+ (* qxsq 0.75) (* qxsq x)) (+ (* qysq 0.75) (* qysq y)))
  (q/stroke "white")
  (q/stroke-weight 2)
  (q/ellipse (+ (* qxsq 0.35) (* qxsq x)) (+ (* qysq 0.35) (* qysq y)) (* 0.15 qxsq) (* 0.15 qysq))
  (q/ellipse (+ (* qxsq 0.65) (* qxsq x)) (+ (* qysq 0.35) (* qysq y)) (* 0.15 qxsq) (* 0.15 qysq))
  (q/no-stroke))

(defn draw-food
  [x y]
  (q/no-stroke)
  (q/fill "black")
  (q/rect (* qxsq x) (* qysq y) qxsq qysq)
  (q/fill "green")
  (q/ellipse (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.25 qxsq) (* 0.25 qysq)))

(defn draw-exit [x y]
  (q/no-stroke)
  (q/fill "black") (q/rect (* qxsq x) (* qysq y) qxsq qysq)
  (q/fill "green")
  (q/ellipse (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 1 qxsq) (* 1 qysq))
  (q/fill "blue")
  (q/ellipse (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.625 qxsq) (* 0.625 qysq))
  (q/fill "red")
  (q/ellipse (+ (/ qxsq 2) (* qxsq x)) (+ (/ qysq 2) (* qysq y)) (* 0.25 qxsq) (* 0.25 qysq)))

(defn draw-wall [x y]
  (q/no-stroke)
  (let [lines 10]
    (doseq [a (range lines)]
      (if (zero? (rem a 2)) (q/fill "red") (q/fill "black"))
      (q/rect (* qxsq x) (* qysq (+ y (/ a lines))) qxsq (* (/ 1 lines) qysq)))))

(defn draw-state [state]
  (let [table (:table state)]
    (doseq [x (range board-size-x) y (range board-size-y)]
      (case (get-value table x y)
        :p (draw-pacman x y)
        :f (draw-food x y)
        :i (draw-exit x y)
        :h (draw-ghost x y true)
        :g (draw-ghost x y false)
        :e (do (q/fill "black") (q/no-stroke) (q/rect (* qxsq x) (* qysq y) qxsq qysq))
        :w (do (q/fill "red") (draw-wall x y))))
    (if (:end state)
      (if (zero? (count-element table :i))
        (status-box table "You've won! Press r to restart.")
        (status-box table "Game over! Press r to restart."))
      (status-box table "Press r to restart."))))


; this function is called in index.html
(defn ^:export run-sketch []
  (q/defsketch pacman
               :host "pacman"
               :size [(+ 200 quil-size-x) quil-size-y]
               ; setup function called only once, during sketch initialization.
               :setup setup
               ; update-state is called on each iteration before draw-state.
               :update update-state
               :draw draw-state
               :key-pressed on-key-down
               :key-released on-key-up
               ; This sketch uses functional-mode middleware.
               ; Check quil wiki for more info about middlewares and particularly
               ; fun-mode.
               :middleware [m/fun-mode]))

; uncomment this line to reset the sketch:
; (run-sketch)
