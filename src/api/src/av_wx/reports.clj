(ns av-wx.reports
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils])
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.pprint]
        [clojure.tools.trace]))

(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")
(def taf-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=tafs&requestType=retrieve&format=csv&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")

(def taf-field-types {#(BigDecimal. %) ["latitude", "longitude", "elevation_m", "visibility_statute_mi", "altim_in_hg", "sfc_temp_c",
                                        "max_temp_c", "min_temp_c"],
                     #(Integer/parseInt %) ["probability", "wind_dir_degrees", "wind_speed_kt", "wind_gust_kt", "wind_shear_hgt_ft_agl",
                                            "wind_shear_dir_degrees", "wind_shear_speed_kt", "vert_vis_ft", "cloud_base_ft_agl",
                                            "turbulence_min_alt_ft_agl", "turbulence_max_alt_ft_agl", "icing_min_alt_ft_agl",
                                            "icing_max_alt_ft_agl"]})

(def metar-field-types {#(BigDecimal. %) ["maxT24hr_c", "pcp6hr_in", "snow_in", "sea_level_pressure_mb", "pcp3hr_in", "minT_c", "altim_in_hg",
                                          "three_hr_pressure_tendancy_mb", "elevation_m", "latitude", "longitude", "minT24hr_c", "maxT_c",
                                          "visibility_statute_mi", "temp_c", "pcp24hr_in", "precip_in", "dewpoint_in", "dewpoint_c"],
                        #(Integer/parseInt %) ["wind_gust_kt", "wind_speed_kt", "cloud_base_ft_agl", "vert_vis_ft", "wind_dir_degrees"]})

(defn remove-empty-values [csvmap]
  (into {} (remove #(or (nil? (val %)) (and (string? (val %)) (clojure.string/blank? (val %)))) csvmap)))

(defn- fix-field-types [rows types]
  (map
   (fn [row]
     (reduce-kv #(utils/update-vals %1 %3 %2) (remove-empty-values row) types))
   rows))

(defn parse-report [csvlines field-types]
  (let [csvrows (parse-csv (subs csvlines (.indexOf csvlines "raw_text")))]
    (fix-field-types (map #(zipmap (first csvrows) %) (rest csvrows)) field-types)))

(defn get-geoip-data [ipaddr]
  (let [{:keys [error status headers body]} @(http/get (str "http://freegeoip.net/json" ipaddr) {:as :text})]
   (if error
     nil
     (parse-string body true))))

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
     (parse-report body metar-field-types))))

(defn get-tafs [search]
  (let [{:keys [error status headers body]} @(http/get (str taf-url search) {:as :text})]
    (if error
      (println "ERROR!" error)
      (parse-report body taf-field-types))))

;(pprint (get-tafs "KSFO+KSTL"))
;(pprint (get-metars "KSFO"))
;(pprint (mapv (get-geoip-data "66.249.78.223") ["latitude" "longitude"]))
;(pprint (append-geo-data (get-metars "KSFO") (mapv (get-geoip-data "66.249.78.223") ["latitude" "longitude"])))
