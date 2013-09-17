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

(defn find-stations [latitude longitude mode]
  (mapv
    #(hash-map "icao" (get-in % ["obj" "icao"])
               "name" (get-in % ["obj" "name"]))
    (get-in (mg/command (sorted-map
                         :geoNear "stations"
                         :near {:type "Point", :coordinates [longitude, latitude]}
                         :query {mode true}
                         :limit 15
                         :spherical true))
            ["results"])))

(defn find-stations-zipcode [zipcode mode]
  (let [[lon lat] ((mc/find-one-as-map "zipcodes" {:_id (str zipcode)} {:_id 0, :loc 1}) :loc)]
    (find-stations lat lon mode)))

(defn find-stations-ip [ipaddr mode]
  (let [{lat :latitude lon :longitude} (reports/get-geoip-data ipaddr)]
    (find-stations lat lon mode)))

;(pprint (find-stations 37.62 -122.37 "metar"))
;(pprint (find-stations 37.62 -122.37 "metar"))

;(pprint (find-zipcode 63111))

;(pprint (find-stations-zipcode 63111 "metar"))
;(pprint (find-stations-ip "216.55.25.70" "metar"))
