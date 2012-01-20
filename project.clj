(defproject ldoce "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[clojure "1.3.0"]
                 [me.shenfeng/async-ring-adapter "1.0.1"]
                 [org.clojure/data.json "0.1.2"]
                 [ring/ring-core "1.0.1"]
                 [org.jsoup/jsoup "1.6.1"]
                 [compojure "1.0.0"]]
  :warn-on-reflection true
  :jvm-opts ["-XX:+UseCompressedOops"
             "-Dsun.net.inetaddr.ttl=0"
             "-Djava.net.preferIPv4Stack=true"
             "-XX:+TieredCompilation"
             "-XX:-DisableExplicitGC"
             "-XX:+UseCompressedStrings"
             ;; "-XX:-UseLoopPredicate"
             "-Xmx2048m"
             "-Xms2048m"]
  :javac-options {:source "1.6" :target "1.6" :debug "true" :fork "true"}
  :java-source-path "src/java"
  :dev-dependencies [[swank-clojure "1.4.0-SNAPSHOT"]
                     [junit/junit "4.8.2"]])
