(ns av-wx.core
  (:use [compojure.route :only [files not-found]]
        [compojure.handler :only [site]]
        [compojure.core :only [defroutes GET POST ANY context]]
        [org.httpkit.server]
        [clojure.pprint]
        [ring.middleware.json :only [wrap-json-response]]
        [ring.util.response :only [response]])
  (:require [ring.middleware.reload :as reload]
            [av-wx.reports :as reports]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils]
            [taoensso.timbre :as timbre :refer (trace debug info warn error fatal spy with-log-level)]
            [taoensso.timbre.profiling :as profiling :refer (p profile sampling-profile)]))

(defn show-index-page [args] "Meow!")

; If httparams were passed, just return those
; Otherwise use the geoip service
(defn get-geo-data
  ([ip {:keys [latitude longitude] :as httploc}]
   (if (and latitude longitude)
     [(BigDecimal. latitude) (BigDecimal. longitude)]
     (get-geo-data ip)))
  ([ip]
   (if-let [geoip-data (reports/get-geoip-data ip)]
    (mapv geoip-data [:latitude :longitude]) nil)))

(defn build-response [reports ipaddr httploc]
  (if-let [geo-data (get-geo-data ipaddr httploc)]
    {"reports"  (reports/append-geo-data reports geo-data),
      "location" geo-data} reports))

(defn get-metar [search qparams remote-addr]
  (let [httploc (select-keys qparams [:latitude :longitude])]
    (sampling-profile :info 0.10 :get-metar
                      (response
                       (build-response
                        (p :get-metar (reports/get-metars search)) remote-addr httploc)))))

(defn get-taf [search qparams remote-addr]
  (let [httploc (select-keys qparams [:latitude :longitude])]
    (sampling-profile :info 0.10 :get-taf
                      (response
                       (build-response
                        (p :get-taf (reports/get-tafs search)) remote-addr httploc)))))

(defn in-dev? [args] true)

(defroutes all-routes
  (GET "/" [] show-index-page)
  (GET "/metar/:search" [search :as {qparams :params remote-addr :remote-addr}] (get-metar search qparams remote-addr))
  (GET "/taf/:search"  [search :as {qparams :params remote-addr :remote-addr}] (get-taf search qparams remote-addr))
  ;(GET "/_loging/:level" [level] (timbre/set-level! level))
  (not-found "Fer oh fer, page not fernd!"))

(defn -main [& args] ;; entry point, lein run will pick up and start from here
  (let [handler (if (in-dev? args)
                  (reload/wrap-reload (site #'all-routes)) ;; only reload when dev
                  (site all-routes))]
    (run-server (wrap-json-response handler) {:port 8080})))
