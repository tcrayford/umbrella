(ns umbrella.transmogrification
  (:use clojure.contrib.repl-utils))

(defn analyze [node]
  (clojure.lang.Compiler/analyze clojure.lang.Compiler$C/EVAL node))

(defn all-vars-bound-in-node [node]
  (->> (analyze node)
       .fexpr
       .methods
       first
       .locals
       keys
       (map #(.sym %))))
