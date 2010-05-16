(ns umbrella.core
  (:gen-class)
  (:use clojure.contrib.io)
  (:use clojure.contrib.pprint)
  (:use clojure.contrib.find-namespaces)
  (:use clojure.contrib.command-line)
  (:use clojure.contrib.shell-out)
  (:use clojure.walk)
  (:use clojure.set)
  (:import (clojure.lang RT)
           (java.io LineNumberReader InputStreamReader PushbackReader)))

(defn examined-dir []
  (java.io.File. (str (reduce str (drop-last (sh "pwd"))) "/src")))

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
            (if-let [code (get-source-from-var v)]
              (assoc accum v (read-string code))
              accum))
          {} (vals (ns-publics ns))))

(defn all-vars-in-namespaces [& nss]
  (merge (map source-for-all-vars-in-ns nss)))

(defn all-vars-in-dir [dir]
  (apply all-vars-in-namespaces (find-namespaces-in-dir dir)))

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

(defn all-sub-nodes [coll]
  (filter (complement symbol?)
          (tree-seq seq? seq coll)))

(defn longest-sub-node [x y]
  (->> (map smart-lcs (all-sub-nodes x) (all-sub-nodes y))
       (filter (complement nil?))
       (sort-by node-count)
       (last)))

(defn compare-all-subnodes [[node-map]]
  (filter (complement nil?)
          (map
           (fn [[k v]]
             (map
              (fn [[ke va]]
                {:comparison (longest-sub-node v va)
                 :from k :to ke})
              (dissoc node-map k)))
           node-map)))

(defn count-nodes-report [[node-map]]
  (reduce + (map second (map (fn [[k v]] (vector k (node-count v))) node-map))))

(defn comparison-sort-val [{c :comparison}]
  (if (nil? c)
    -1
    (node-count c)))

(defn compare-all-nodes-report [node-map]
  (map #(last
         (sort-by
          comparison-sort-val %))
       (compare-all-subnodes node-map)))

(defn -main [& args]
  (with-command-line args
    []
    []
    (println
     (with-out-str
       (pprint
       (compare-all-nodes-report
        (all-vars-in-dir (examined-dir))))))))