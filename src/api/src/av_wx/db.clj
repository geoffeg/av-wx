(ns av-wx.db
  (:require [monger.core :as mg]
            [av-wx.utils :as utils]
            [av-wx.reports :as reports]
            [monger.collection :as mc])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:use [clojure.pprint]))

(let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 3)
      ^ServerAddress sa  (mg/server-address (get-in utils/conf [:mongo :host]) (get-in utils/conf [:mongo :port]))]
  (mg/connect! sa opts))

(mg/set-db! (mg/get-db (get-in utils/conf [:mongo :db])))

(defn get-airport-info [airports]
  (let [mongo-search {"ident" { "$in" airports}}]
    (mc/find-maps "airports" mongo-search {:_id 0, :name 1, :ident 1})))

(defn find-stations [mode coords]
  (if-let [results (mg/command (sorted-map
                         :geoNear "stations"
                         :near {:type "Point", :coordinates [(coords 1), (coords 0)]}
                         :query {mode true}
                         :limit 15
                         :spherical true))]
    (if (contains? results "results")
      (mapv #(get-in % ["obj" "icao"]) (get-in results ["results"]))
      nil)
    nil))

(defn find-coords-zipcode [mode zipcode]
  (if-let [loc (mc/find-one-as-map "zipcodes" {:_id (str zipcode)} {:_id 0, :loc 1})]
    [(last (loc :loc)) (first (loc :loc))] nil))

(defn find-coords-ip [mode ipaddr]
  (if-let [{lat :latitude lon :longitude} (reports/get-geoip-data ipaddr)]
    [lat lon] nil))

(defn append-airport-info [reports]
  (let [report-airports (mapv #(% "station_id") reports)
        airport-info (get-airport-info report-airports)]
    (clojure.set/join (clojure.set/rename airport-info {:ident "station_id"}) reports)))
