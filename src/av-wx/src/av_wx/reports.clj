 (ns av-wx.reports
  (:require [org.httpkit.client :as http]
            [cheshire.core :refer :all]
            [av-wx.utils :as utils]
            [clojure.xml :as x]
            [clojure.zip :as z])
  (:use [clojure-csv.core :only [parse-csv]]
        [clojure.pprint]))

(def metar-url "http://aviationweather.gov/adds/dataserver_current/httpparam?dataSource=metars&requestType=retrieve&format=xml&hoursBeforeNow=4&mostRecentForEachStation=true&stationString=")
;(def metar-url "http://geoffeg.org/mt.txt?")

(defn metar-zipper
  [url]
  (-> url x/parse z/xml-zip z/children))

(defn tag-kv
  [nodes]
  (when (vector? nodes)
    (reduce
      (fn [xs {:keys [tag content]}]
        (assoc xs tag
                  (if (map? (first content))
                      (tag-kv content)
                      (first content))))
      {}
      nodes)))

(defn parse-metar
  [url]
  (->> (metar-zipper url)
       (filterv #(= :data (:tag %)))
       first
       :content
       (mapv #(tag-kv (% :content)))))

(defn get-xml [search]
  (let [{:keys [error status headers body]} @(http/get (str metar-url search) {:as :text})]
  (if error
    (println "ERROR!" error)
    (clojure.string/replace body "http://aviationweather.gov/adds/schema/metar1_2.xsd" "http://new.aviationweather.gov/doc/schema/metar1_2.xsd"))))

;(defn parse-metar [csvdata]
;  (let [csvrows (parse-csv (subs csvdata (.indexOf csvdata "raw_text")))]
;  (map #(zipmap (first csvrows) %) (rest csvrows))))

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
    (append-geo-data
     (parse-metar body)
     (mapv
      (get-geoip-data "108.73.45.165") ["latitude", "longitude"])))))

;(println (get-metars "KSFO"))
;(get-geoip-data "108.73.45.165")
;(pprint (parse-metar (str metar-url "KSFO")))
(pprint (parse-metar (utils/string-to-stream(get-xml "KSFO"))))