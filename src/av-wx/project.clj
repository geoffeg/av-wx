(defproject av-wx "0.1.0-SNAPSHOT"
  :description "av-wx: aviation weather"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.novemberain/monger "1.6.0"]
                 [http-kit "2.1.8"]
                 [compojure "1.1.5"]
                 [ring/ring-devel "1.1.8"]
                 [ring/ring-core "1.1.8"]
                 [cheshire "5.2.0"]
                 [ring/ring-json "0.2.0"]]
  :main av-wx.core)
