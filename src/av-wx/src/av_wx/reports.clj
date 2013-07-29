(ns av-wx.reports
  (:require [org.httpkit.client :as http])
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.pprint]))

;(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")
(def metar-url "http://geoffeg.org/mt.txt")

(defn parse-metar [csvdata]
  (let [csvrows (parse-csv (subs csvdata (.indexOf csvdata "raw_text")))]
  (map #(interleave (first csvrows) %) (rest csvrows))))

(defn get-metars [search]
  (let [{:keys [error status headers body]} @(http/get (str metar-url) {:as :text})]
  (if error
    (println "ERROR!")
    (pprint (parse-metar body)))))

(get-metars "KSFO")

