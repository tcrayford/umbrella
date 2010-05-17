(ns umbrella.transmogrification
  (:use clojure.walk))

(defn analyze [node]
  (clojure.lang.Compiler/analyze clojure.lang.Compiler$C/EVAL node))

(defmulti get-to-methods #(class (analyze %)))

(defmethod get-to-methods clojure.lang.Compiler$DefExpr [node]
  (->> (analyze node) .init .target))

(defmethod get-to-methods clojure.lang.Compiler$InvokeExpr [node]
  (->> (analyze node)
       .fexpr))

(defn all-vars-bound-in-node [node]
  (->> (get-to-methods node)
       .methods
       first
       .locals
       keys
       (map #(.sym %))
       (into #{})))

(defn symbol-replacements [symbols]
  "Takes a symbol set and outputs a map of the elems of the symbol set to transmogrified variable name strings"
  (into
   {}
   (map #(vector %1 (symbol (str "transmogd-" %2))) symbols (iterate inc 0))))

(defn construct-replacement-node [new-node old]
  (with-meta
    new-node
    {:old old}))

(defn transmogrify-sub-node [node replacements]
  (let [variable? (into #{} (keys replacements))]
    (if (variable? node)
      (construct-replacement-node (replacements node) node)
      node)))

(defn transmogrify [node]
  (let [replacements (symbol-replacements (all-vars-bound-in-node node))
        variable? (into #{} (keys replacements))]
    (postwalk
     #(transmogrify-sub-node % replacements)
     node)))

(defn maybe-replace-with-old-node [node]
  (if-let [n (:old (meta node))] n node))

(defn untransmogrify [transmogd-node]
  (postwalk
   maybe-replace-with-old-node
   transmogd-node))

