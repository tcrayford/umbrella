(ns umbrella.transmogrification
  (:use clojure.test)
  (:use umbrella.transmogrification :reload-all))

(deftest all_vars_bound_in_node
  (testing "defn binds"
    (is (= (all-vars-bound-in-node '(defn a [b] (let [x 1] (+ b x)))) '#{b x a})))
  (testing "let binds"
    (is (= (all-vars-bound-in-node '(let [a 1] a)) '#{a}))))

(deftest transmogrify_test
  (is (= (transmogrify '(defn a [c] c))
       '(defn transmogd-0 [transmogd-1] transmogd-1))))

(deftest untransmogrify_test
  (is (=
       (untransmogrify (transmogrify '(defn a [b] b)))
       '(defn a [b] b))
      "Untransmogrifying a node that is transmogrified should return the original node")
  (is (=
       (untransmogrify '(defn a [b] b))
       '(defn a [b] b))
      "Untransmogrifying a node that isn't transmogrified should just return the node"))
