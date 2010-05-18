(ns umbrella.core
  (:use clojure.contrib.io)
  (:use clojure.contrib.pprint)
  (:use clojure.contrib.find-namespaces)
  (:use clojure.walk)
  (:use umbrella.transmogrification)
  (:import (clojure.lang RT)
           (java.io LineNumberReader InputStreamReader PushbackReader)))

(defn examined-dir []
  (java.io.File. (str (System/getProperty "user.dir") "/src")))

(defn get-source-from-var
  "Returns a string of the source code for the given symbol, if it can
find it. This requires that the symbol resolve to a Var defined in
a namespace for which the .clj is in the classpath. Returns nil if
it can't find the source.
Example: (get-source-from-var 'filter)"
  [v] (when-let [filepath (:file (meta v))]
        (when-let [strm (.getResourceAsStream (RT/baseLoader) filepath)]
          (with-open [rdr (LineNumberReader. (InputStreamReader. strm))]
            (dotimes [_ (dec (:line (meta v)))] (.readLine rdr))
            (let [text (StringBuilder.)
                  pbr (proxy [PushbackReader] [rdr]
                        (read [] (let [i (proxy-super read)]
                                   (.append text (char i))
                                   i)))]
              (read (PushbackReader. pbr))
              (str text))))))

(defn source-for-all-vars-in-ns [ns]
  (reduce (fn [accum v]
            (if-let [code  (get-source-from-var v)]
              (assoc accum v (transmogrify (drop 2 (read-string code)) ns))
              accum))
          {} (vals (ns-publics ns))))

(defn all-vars-in-dir [dir]
  (apply merge (map source-for-all-vars-in-ns (find-namespaces-in-dir dir))))

(defn node-count [node]
  (count (flatten node)))

(defn first= [x y]
  (= (first x) (first y)))

(defn longest [xs]
  (last (sort-by count xs)))

(def lcs ;;longest common subsequence, used for diffing
     (memoize
      (fn [xstr ystr]
        (if (or (empty? xstr) (empty? ystr))
          (empty xstr)
          (let [xs (next xstr)
                ys (next ystr)]
            (if (first= xstr ystr)
              (conj (lcs xs ys) (first xstr))
              (longest [(lcs xstr ys) (lcs xs ystr)])))))))

(defn smart-lcs [x y]
  (if (or (not (seq? y)) (not (seq? x)))
    nil
    (lcs x y)))

(defn expand-tree [node]
  "Expands a node into a seq of all its sub-node"
  (tree-seq #(some coll? %)
            #(filter coll? %)
            node))

(def all-sub-nodes expand-tree)

(defn longest-sub-node [x y]
  (->> (map smart-lcs (all-sub-nodes x) (all-sub-nodes y))
       (filter (complement nil?))
       (sort-by node-count)
       (last)))

(defn compare-all-subnodes [node-map]
  (filter (complement nil?)
          (map
           (fn [[k v]]
             (map
              (fn [[ke va]]
                {:repetition (longest-sub-node v va)
                 :from k :to ke})
              (dissoc node-map k)))
           node-map)))

;; node-map is of the form {var node, var node...}
(defn count-nodes-report [node-map]
  (reduce + (map second (map (fn [[k v]] (vector k (node-count v))) node-map))))

(defn comparison-sort-val [{c :repetition}]
  (node-count c))

(defn compare-all-nodes-report [node-map]
  (map #(last
         (sort-by
          comparison-sort-val %))
       (compare-all-subnodes node-map)))

(defn var->ns [var]
  (.name (:ns (meta var))))

(defn var->line [var]
  (:line (meta var)))

(defn var->location-spec [var]
  (str (var->ns var) var))

(defn elisp-problem-report [problem]
  `((:line ~(var->line (:from problem)))
    (:text ~(reduce str (drop 1(drop-last (str  (untransmogrify (:repetition problem)))))))
    (:message ~(str "duplicated with " (:to problem)))))

(defn umbrella-run [examined-ns]
  (reduce
   (fn [problems p]
     (conj problems
           (elisp-problem-report p)))
   '()
   (->> (compare-all-nodes-report (source-for-all-vars-in-ns examined-ns))
        (filter #(= (var->ns (:from %)) examined-ns))
        (filter :repetition)
        (filter #(not= (count (:repetition %)) 1)))))
