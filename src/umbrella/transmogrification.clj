(ns umbrella.transmogrification
  (:use clojure.walk)
  (:use clojure.contrib.with-ns))

(defmacro protect-from-exceptions [expr]
  `(try ~expr (catch Exception ~'e nil)))

(defn analyze [node]
  (protect-from-exceptions (clojure.lang.Compiler/analyze clojure.lang.Compiler$C/EVAL node)))

(defmulti get-to-methods #(class (analyze %)))

 (defmethod get-to-methods clojure.lang.Compiler$InvokeExpr [node]
   (->> (analyze node)
        .fexpr))

(defmethod get-to-methods clojure.lang.Compiler$DefExpr [node]
  (->> (analyze node) .init .target))

(defmethod get-to-methods :default [node]
  nil)

(defn all-vars-bound-in-node [node]
  (if-let [it (protect-from-exceptions (get-to-methods node))]
   (->> it
        .methods
        first
        .locals
        keys
        (map #(.sym %))
        (into #{}))
   #{}))

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

(defn find-bound-vars [node ns]
  (->> (flatten node)
       (filter symbol?)
       (filter (complement #(ns-resolve ns %)))
       (into #{})))

(defn transmogrify [node ns]
  (let [replacements (symbol-replacements (find-bound-vars node ns))]
   (postwalk
    #(transmogrify-sub-node % replacements)
    node)))

(defn maybe-replace-with-old-node [node]
  (if-let [n (:old (meta node))] n node))

(defn untransmogrify [transmogd-node]
  (postwalk
   maybe-replace-with-old-node
   transmogd-node))
