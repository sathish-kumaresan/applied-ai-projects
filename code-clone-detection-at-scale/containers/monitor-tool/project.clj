(defproject monitor-tool "0.1.0-SNAPSHOT"
  :description "Database monitoring tool for Clone Detector"
  :url "http://localhost:9090/monitoring"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.novemberain/monger "3.6.0"]
                 [org.clojure/core.async "1.6.673"]
                 [ring "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-json "0.5.1"]]
  :main ^:skip-aot monitor-tool.monitoring
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
