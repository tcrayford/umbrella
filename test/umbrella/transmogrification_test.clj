(ns umbrella.transmogrification
  (:use clojure.test)
  (:use umbrella.transmogrification :reload-all))

(deftest all_vars_bound_in_node
  (testing "defn binds"
   (is (= (all-vars-bound-in-node '(defn a [b] b)) '(b))))
  (testing "let binds"
   (is (= (all-vars-bound-in-node '(let [a 1] a)) '(a)))))
