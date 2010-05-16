(defproject umbrella "0.0.1"
  :description "For keeping your code dry (cheers Giles)."
  :dependencies [[org.clojure/clojure
                  "1.2.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib
                  "1.2.0-SNAPSHOT"]]
  :dev-dependencies [[leiningen/lein-swank "1.2.0-SNAPSHOT"]
                     [clojure_refactoring "0.1.5"]]
  :main umbrella.core)
