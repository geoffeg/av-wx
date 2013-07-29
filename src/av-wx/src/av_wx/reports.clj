(ns av-wx.reports
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer :all])
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.pprint]))

(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")
;(def metar-url "http://geoffeg.org/mt.txt")

(defn parse-metar [csvdata]
  (let [csvrows (parse-csv (subs csvdata (.indexOf csvdata "raw_text")))]
  (map #(zipmap (first csvrows) %) (rest csvrows))))

(defn get-metars [search]
  (let [{:keys [error status headers body]} @(http/get (str metar-url search) {:as :text})]
  (if error
    (println "ERROR!" error)
    (parse-metar body))))

(defn get-geoip-data [ipaddr]
  (let [{:keys [error status headers body]} @(http/get (str "http://freegeoip.net/json" ipaddr) {:as :text})]
   (if error
     (println "ERROR!")
     (parse-string body))))

(get-metars "KSFO")
(get-geoip-data "108.73.45.165")