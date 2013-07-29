(ns av-wx.core
  (:use [compojure.route :only [files not-found]]
        [compojure.handler :only [site]]
        [compojure.core :only [defroutes GET POST ANY context]]
        [org.httpkit.server]
        [ring.middleware.json :only [wrap-json-response]]
        [ring.util.response :only [response]])
  (:require [ring.middleware.reload :as reload]
            [av-wx.reports :as reports]
            [cheshire.core :refer :all]))

(defn show-index-page [args] "Meow!")

(defn get-metar [search] (response (reports/get-metars search)))

(defn get-taf [search] (response {:search search}))

(defn in-dev? [args] true)

(defroutes all-routes
  (GET "/" [] show-index-page)
  (GET "/metar/:search" [search] (get-metar search))
  (GET "/taf/:search" [search] (get-taf search))
  (not-found "Fer oh fer, page not fernd!"))

(defn -main [& args] ;; entry point, lein run will pick up and start from here
  (let [handler (if (in-dev? args)
                  (reload/wrap-reload (site #'all-routes)) ;; only reload when dev
                  (site all-routes))]
    (run-server (wrap-json-response handler) {:port 8080})))