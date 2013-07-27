(ns av-wx.reports
  (:require [org.httpkit.client :as http]))

(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")

(defn parse-metar [csvdata]
  (let [rdr (clojure.java.io/reader csvdata)]
  (doseq [line (line-seq rdr)]
    (println line))))

(defn get-metars [search]
  (let [{:keys [error status headers body]} @(http/get (str metar-url "KSTL"))]
  (if error
    (println "ERROR!")
    (println "SUCCESS" (parse-metar body)))))

(get-metars "KSFO")

