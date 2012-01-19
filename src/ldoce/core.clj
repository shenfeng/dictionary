(ns ldoce.core
  (:use [ring.adapter.netty :only [run-netty]])
  (:import me.shenfeng.Extrator))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (println "shutdown netty server....")
    (@server)
    (reset! server nil)))


(defn handler [req]
  (let [html (slurp "/home/feng/www.ldoceonline.com/dictionary/go_1")
        result (Extrator/e html)]
    {:status 200
     :headers {"Content-Type" "text/html"
               "Content-Encoding" "gzip"}
     :body result}))

(defn start-server [{:keys [port worker]}]
  (stop-server)
  (reset! server (run-netty handler {:port port
                                     :worker worker}))
  (println "netty server start at port" port))

(defn -main [& arg]
  (start-server {:port 8080 :worker 1}))
