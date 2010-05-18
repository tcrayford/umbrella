(ns umbrella.transmogrification
  (:use clojure.test)
  (:use umbrella.transmogrification :reload-all))

;; (deftest all_vars_bound_in_node
;;   (testing "defn binds"
;;     (is (= (all-vars-bound-in-node '(defn a [b] (let [x 1] (+ b x)))) '#{b x a})))
;;   (testing "let binds"
;;     (is (= (all-vars-bound-in-node '(let [a 1] a)) '#{a})))
;;   (testing "empty nodes"
;;     (is (= (all-vars-bound-in-node '()) #{}))))

(deftest transmogrify_test
  (is (= (transmogrify '(defn a [c] c))
       '(defn a [transmogd-0] transmogd-0))))

(deftest test_umbrella_other
  (is (= (transmogrify '(defn foo [a b] (+ a b))))))

(deftest untransmogrify_test
  (is (=
       (untransmogrify (transmogrify '(defn a [b] b)))
       '(defn a [b] b))
      "Untransmogrifying a node that is transmogrified should return the original node")
  (is (=
       (untransmogrify '(defn a [b] b))
       '(defn a [b] b))
      "Untransmogrifying a node that isn't transmogrified should just return the node"))

