(ns av-wx.db
  (:require [monger.core :as mg]
            [av-wx.utils :as utils]
            [monger.collection :as mc])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:use [clojure.pprint]))

(let [^MongoOptions opts (mg/mongo-options :threads-allowed-to-block-for-connection-multiplier 300)
      ^ServerAddress sa  (mg/server-address (get-in utils/conf [:mongo :host]) (get-in utils/conf [:mongo :port]))]
  (mg/connect! sa opts))

(mg/set-db! (mg/get-db (get-in utils/conf [:mongo :db])))

(defn find-stations [latitude longitude mode]
  (first (get-in (mg/command (sorted-map
   :geoNear "stations"
   :near {:type "Point", :coordinates [longitude, latitude]}
   :query {mode true}
   :limit 1
   :spherical true)) ["results"])))

(defn find-zipcode [zipcode]
  ((mc/find-one-as-map "zipcodes" {:_id (str zipcode)} {:_id 0, :loc 1}) :loc))

;(pprint (get-in (find-stations 37.62 -122.37 "metar") ["obj" "icao"]))
;(pprint (find-stations 37.62 -122.37 "metar"))

;(pprint (find-zipcode 63111))