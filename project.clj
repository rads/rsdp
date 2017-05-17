(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :repositories
  [["clojars" {:url "https://repo.clojars.org/"}]
   ["maven-central" {:url "https://repo1.maven.org/maven2"}]]
  :dependencies
  [[adzerk/boot-cljs "2.0.0" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [adzerk/boot-test "1.2.0" :scope "test"]
   [aleph "0.4.3"]
   [amalloy/ring-buffer "1.2.1"]
   [cljs-http "0.1.42"]
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [com.cognitect/transit-clj "0.8.300"]
   [com.stuartsierra/component "0.3.2"]
   [gloss "0.2.6"]
   [integrant "0.3.3"]
   [io.replikativ/kabel "0.2.0"]
   [io.replikativ/superv.async "0.2.6"]
   [org.clojure/clojure "1.9.0-alpha15"]
   [org.clojure/clojurescript "1.9.521"]
   [org.clojure/core.async "0.3.441"]
   [org.clojure/core.match "0.3.0-alpha4"]
   [org.clojure/test.check "0.9.0"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [pandeiro/boot-http "0.7.6"]
   [reagent "0.6.1"]
   [weasel "0.7.0" :scope "test"]]
  :source-paths
  ["src"
   "/Users/rads/.boot/cache/tmp/Users/rads/Dropbox/src/rads/rsdp/3lm/6ppmw"
   "test"]
  :resource-paths
  ["resources"])