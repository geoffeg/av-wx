(ns av-wx.core
  (:use [compojure.route :only [files not-found]]
        [compojure.handler :only [site]]
        [compojure.core :only [defroutes GET POST ANY context]]
        [org.httpkit.server]
        [ring.middleware.json :only [wrap-json-response]]
        [ring.util.response :only [response]])
  (:require [ring.middleware.reload :as reload]
            [av-wx.reports :as reports]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils]))

(defn show-index-page [args] "Meow!")

; If httparams were passed, just return those
; Otherwise use the geoip service
(defn get-geo-data
  ([ip httparams]
    (get-geo-data ip))
  ([ip]
    (reports/get-geoip-data ip)))

(defn build-response [reports ipaddr geoloc]
  (let [geo-data (get-geo-data ipaddr)]
    {"reports"  (reports/append-geo-data reports (mapv geo-data ["latitude" "longitude"])),
      "location" geo-data}))

(defn get-metar [search]
  (response (build-response (reports/get-metars search) "108.73.45.165" nil)))

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