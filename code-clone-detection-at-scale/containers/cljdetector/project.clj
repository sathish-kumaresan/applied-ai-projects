(defproject cljdetector "0.1.0-SNAPSHOT"
  :description "Application for Clone Detector"
  :url "http://localhost:8081/monitoring"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clj-commons/digest "1.4.100"]
                 [com.novemberain/monger "3.6.0"]
                 [org.slf4j/slf4j-nop "2.0.7"]
                 [ring "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-json "0.5.1"]]
  :main ^:skip-aot cljdetector.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
