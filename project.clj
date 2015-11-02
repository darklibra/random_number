
(defproject random-number "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [racehub/om-bootstrap "0.5.3"]
                 [org.omcljs/om "0.9.0"]
                 [prismatic/om-tools "0.3.12"]]

  :plugins [[lein-cljsbuild "1.1.0"]]
  :source-paths ["src/clj"]
  :cljsbuild { :builds [{:source-paths ["src/cljs"]
                        :compiler {:output-to "resources/js/core.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]})
