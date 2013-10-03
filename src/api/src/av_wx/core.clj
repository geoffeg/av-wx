(ns av-wx.core
  (:use [compojure.route :only [files not-found]]
        [compojure.handler :only [site]]
        [compojure.core :only [defroutes GET POST ANY context]]
        [org.httpkit.server]
        [clojure.pprint]
        [let-else]
        [ring.middleware.json :only [wrap-json-response]]
        [ring.util.response :only [response]])
  (:require [ring.middleware.reload :as reload]
            [av-wx.reports :as reports]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils]
            [av-wx.db :as db]
            [taoensso.timbre :as timbre :refer (trace debug info warn error fatal spy with-log-level)]
            [taoensso.timbre.profiling :as profiling :refer (p profile sampling-profile)]))

(defn show-index-page [args] "Meow!")

(defn get-geo-data [remote-addr geoloc]
  (if (empty? geoloc)
    (let [pos (reports/get-geoip-data remote-addr)] (vector (BigDecimal. (pos :latitude)) (BigDecimal. (pos :longitude))))
    (let [pos (clojure.string/split geoloc #",")] (vector (BigDecimal. (pos 0)) (BigDecimal. (pos 1))))))

(defn geo-response [reports loc]
  (if (empty? loc) reports
  (response
    {"reports" (reports/append-geo-data reports loc),
     "location" loc})))

(defn- error-response [message]
  (response {"ERROR" message}))

(defn get-metar [search qparams remote-addr]
  (let [httploc (get-geo-data remote-addr (get qparams :geo))
        stations (clojure.string/split search #",")]
    (geo-response (reports/get-metars stations) httploc)))

(defn get-taf [search qparams remote-addr]
  (let [httploc (get-geo-data remote-addr (get qparams :geo))
        stations (clojure.string/split search #",")]
      (geo-response (reports/get-tafs stations) httploc)))

(defn get-location [qparams]
    (let [searchtypes {:zipcode #(db/find-coords-zipcode (qparams :type) %)
                       :ip      #(db/find-coords-ip (qparams :type) %)
                       :geo     #(clojure.string/split % #",")}
          [k f] (first (select-keys searchtypes (keys qparams)))]
      (f (qparams k))))

(defn search-weather [qparams remote-addr]
  ; three search methods are supported:
  ;   zipcode=XXXXX      return airports within proximity to zipcode
  ;   geo=lat,lon        return airports within proximity of coords
  ;   ip=XXX.XXX.XXX.XXX airports within geolocation of IP address
  ;   ip=@detect         airports within geolocation of client IP
  (when (= (get qparams :ip) "@detect") (assoc qparams :ip remote-addr))
  (let? [geoloc   (get-location qparams)                    :is-not nil? :else (error-response "could not find location")
         stations (db/find-stations (qparams :type) geoloc) :is-not nil? :else (error-response "no stations found for location")
         reports  (if
                    (= (qparams :type) "metar")
                    (reports/get-metars stations)
                    (reports/get-tafs stations))            :is-not nil? :else (error-response "No reports found")]
        (geo-response reports geoloc)))

(defn in-dev? [args] (get-in utils/conf [:dev]))

(defroutes all-routes
  (GET "/" [] show-index-page)
  (GET "/search" {qparams :params remote-addr :remote-addr} (search-weather qparams remote-addr))
  (GET "/metar/*" [* :as {qparams :params remote-addr :remote-addr}] (get-metar * qparams remote-addr))
  (GET "/taf/*"  [* :as {qparams :params remote-addr :remote-addr}] (get-taf * qparams remote-addr))
  ;(GET "/_logging/:level" [level] (timbre/set-level! level))
  (not-found "Fer oh fer, page not fernd!"))

(defn -main [& args] ;; entry point, lein run will pick up and start from here
  (let [handler (if (in-dev? args)
                  (reload/wrap-reload (site #'all-routes)) ;; only reload when dev
                  (site all-routes))]
    (run-server (wrap-json-response handler) {:port 8080})))
