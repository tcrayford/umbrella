(ns umbrella.core-test
  (:use [umbrella.core] :reload-all)
  (:use [clojure.test]))

(deftest node_count
  (is (= (node-count '(defn a [b] (+ 1 c))) 6)))

(deftest lcs_test
  (is (= (lcs [1 2 3] [1 2 4]) [1 2]))
  (is (= (lcs [1 2 3] [1 2 3 12]) [1 2 3]))
  (is (= (lcs [7 1 2] [9 1 2]) [1 2])))

(deftest expand_tree
  (is (= (expand-tree '(defn x [a b] (+ 1 a)))
         '([a b] (+ 1 a))))
  (is (= (expand-tree '(defn y [a b] (+ 2 a))) '([a b] (+ 2 a))))
  (is (= (expand-tree '(defn z [a b] (+ a (/ a b))))
                      '([a b] (+ a (/ a b)) (/ a b)))))
