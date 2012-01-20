(ns ldoce.core
  (:use [ring.adapter.netty :only [run-netty]]
        [clojure.data.json :only [json-str]])
  (:import me.shenfeng.Extrator
           me.shenfeng.Writter))

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

(defn- list-like? [o]
  (instance? java.util.List o))

(defn- my-bean? [e]
  (re-find #"me" (str (class e))))

(defn- keep? [k v]
  (and (not (= k :class)) v))

(defn decode-bean [c]
  (let [target
        (if (my-bean? c)
          (if (list-like? c) c (bean c)) c)]
    (cond
     (map? target) (into {}
                         (for [[k v] target d (list (decode-bean v))
                               :when (keep? k d)] [k d]))
     (list-like? target) (map decode-bean target)
     :else target)))

(defn item-to-json [c]
  (json-str (decode-bean c)))

(defn get-result []
  (let [html (slurp "/home/feng/www.ldoceonline.com/dictionary/go_1")]
    (Extrator/extract html)))

(defn to-json []
  (let [html (slurp "/home/feng/www.ldoceonline.com/dictionary/go_1")]
    (decode-bean (Extrator/extract html))))

(defn to-json-str [html]
  (json-str (try (decode-bean
                  (Extrator/extract html))
                 (catch Exception e))))

(defn zipped-json-str [html]
  (me.shenfeng.Zipper/zip2
   (json-str (try (decode-bean
                   (Extrator/extract html))
                  (catch Exception e)))
   9))

(defn print-count []
  (println "string" (-> (get-result) str count))
  (println "json" (-> (to-json) json-str count)))


(defn start-server [{:keys [port worker]}]
  (stop-server)
  (reset! server (run-netty handler {:port port
                                     :worker worker}))
  (println "netty server start at port" port))

(defn -main [& arg]
  (start-server {:port 8080 :worker 1}))


(defn split-to-disk []
  (time (Writter/splitToDisk item-to-json)))

(defn process-all []
  (let [files (filter #(.isFile %)
                      (file-seq (java.io.File.
                                 "/home/feng/www.ldoceonline.com/dictionary")))]
    (reduce + (map count
                   (map zipped-json-str
                        (map slurp files))))))
