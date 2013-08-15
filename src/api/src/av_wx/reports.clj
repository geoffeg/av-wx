(ns av-wx.reports
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils])
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.pprint]))

(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")
(def csv-field-types {"maxT24hr_c" #(BigDecimal. %)
                      "pcp6hr_in" #(BigDecimal. %)
                      "snow_in" #(BigDecimal. %)
                      "wind_gust_kt" #(Integer/parseInt %)
                      "sea_level_pressure_mb" #(BigDecimal. %)
                      "pcp3hr_in" #(BigDecimal. %)
                      "minT_c" #(BigDecimal. %)
                      "altim_in_hg" #(BigDecimal. %)
                      "three_hr_pressure_tendancy_mb" #(BigDecimal. %)
                      "elevation_m" #(BigDecimal. %)
                      "latitude" #(BigDecimal. %)
                      "longitude" #(BigDecimal. %)
                      "minT24hr_c" #(BigDecimal. %)
                      "maxT_c" #(BigDecimal. %)
                      "visibility_statute_mi" #(BigDecimal. %)
                      "temp_c" #(BigDecimal. %)
                      "wind_speed_kt" #(Integer/parseInt %)
                      "cloud_base_ft_agl" #(Integer/parseInt %)
                      "vert_vis_ft" #(Integer/parseInt %)
                      "pcp24hr_in" #(BigDecimal. %)
                      "precip_in" #(BigDecimal. %)
                      "dewpoint_in" #(BigDecimal. %)
                      "dewpoint_c" #(BigDecimal. %)
                      "wind_dir_degrees" #(Integer/parseInt %)})

(defn remove-empty-values [csvmap]
  (into {} (remove #(or (nil? (val %)) (and (string? (val %)) (clojure.string/blank? (val %)))) csvmap)))

(defn cast-csv-fields [csvdata]
  (mapv #(remove-empty-values (utils/cast-map % csv-field-types)) csvdata))

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
            "distance-from" (utils/distance-between src-coords report-coords),
            "bearing-to" (utils/bearing-to src-coords report-coords)))
        reports))

(defn get-metars [search]
  (let [{:keys [error status headers body]} @(http/get (str metar-url search) {:as :text})]
  (if error
    (println "ERROR!" error)
     (parse-metar body))))

;(pprint (get-metars "KSFO"))
;(pprint (mapv (get-geoip-data "66.249.78.223") ["latitude" "longitude"]))
;(pprint (append-geo-data (get-metars "KSFO") (mapv (get-geoip-data "66.249.78.223") ["latitude" "longitude"])))