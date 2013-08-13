(ns av-wx.reports
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils])
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.pprint]))

(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")
(def csv-field-types {"maxT24hr_c" #(Float/parseFloat %)
                      "pcp6hr_in" #(Float/parseFloat %)
                      "snow_in" #(Float/parseFloat %)
                      "wind_gust_kt" #(Integer/parseInt %)
                      "sea_level_pressure_mb" #(Float/parseFloat %)
                      "pcp3hr_in" #(Float/parseFloat %)
                      "minT_c" #(Float/parseFloat %)
                      "altim_in_hg" #(Float/parseFloat %)
                      "three_hr_pressure_tendency_mb" #(String %)
                      "three_hr_pressure_tendancy_mb" #(Float/parseFloat %)
                      "elevation_m" #(Float/parseFloat %)
                      "latitude" #(Float/parseFloat %)
                      "longitude" #(Float/parseFloat %)
                      "minT24hr_c" #(Float/parseFloat %)
                      "maxT_c" #(Float/parseFloat %)
                      "visibility_statute_mi" #(Float/parseFloat %)
                      "temp_c" #(Float/parseFloat %)
                      "wind_speed_kt" #(Integer/parseInt %)
                      "cloud_base_ft_agl" #(Integer/parseInt %)
                      "vert_vis_ft" #(Integer/parseInt %)
                      "pcp24hr_in" #(Float/parseFloat %)
                      "precip_in" #(Float/parseFloat %)
                      "dewpoint_in" #(Float/parseFloat %)
                      "wind_dir_degrees" #(Integer/parseInt %)})

(defn cast-csv-field [csvmap]
  (reduce-kv
   (fn [acc k v]
     (update-in acc [k] #(if-not (clojure.string/blank? %) (v %))))
   csvmap csv-field-types))

(defn cast-csv-fields [csvdata]
  (mapv cast-csv-field csvdata))

(defn parse-metar [csvdata]
  (let [csvrows (parse-csv (subs csvdata (.indexOf csvdata "raw_text")))]
  (cast-csv-fields (map #(zipmap (first csvrows) %) (rest csvrows)))))

(defn get-geoip-data [ipaddr]
  (let [{:keys [error status headers body]} @(http/get (str "http://freegeoip.net/json" ipaddr) {:as :text})]
   (if error
     (println "ERROR!")
     (parse-string body))))

(defn append-geo-data [reports src-coords]
  (mapv #(let [report-coords [(% "latitude") (% "longitude")]]
          (assoc %
            "distance" (utils/distance-between src-coords report-coords),
            "bearing" (utils/bearing-to src-coords report-coords)))
        reports))

(defn get-metars [search]
  (let [{:keys [error status headers body]} @(http/get (str metar-url search) {:as :text})]
  (if error
    (println "ERROR!" error)
     (parse-metar body))))

;(pprint (get-metars "KSFO"))
;(pprint (get-geoip-data "108.73.45.165"))