(ns av-wx.db
  (:require [monger.core :as mg]
            [av-wx.utils :as utils]
            [av-wx.reports :as reports]
            [monger.collection :as mc])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:use [clojure.pprint]))

(let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 300)
      ^ServerAddress sa  (mg/server-address (get-in utils/conf [:mongo :host]) (get-in utils/conf [:mongo :port]))]
  (mg/connect! sa opts))

(mg/set-db! (mg/get-db (get-in utils/conf [:mongo :db])))

(defn find-stations [coords mode]
  (mapv
#(get-in % ["obj" "icao"])
   (get-in (mg/command (sorted-map
                         :geoNear "stations"
                         :near {:type "Point", :coordinates [(coords 1), (coords 0)]}
                         :query {mode true}
                         :limit 15
                         :spherical true))
            ["results"])))

(defn find-coords-zipcode [zipcode mode]
  (let [[lon lat] ((mc/find-one-as-map "zipcodes" {:_id (str zipcode)} {:_id 0, :loc 1}) :loc)]
    [lat lon]))

(defn find-coords-ip [ipaddr mode]
  (let [{lat :latitude lon :longitude} (reports/get-geoip-data ipaddr)]
    [lat lon]))
