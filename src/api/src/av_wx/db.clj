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
  (mg/command (sorted-map
   :geoNear "stations"
   :near {:type "Point", :coordinates [longitude, latitude]}
   :query {"metar" true}
   :limit 1
   :spherical true)))

(defn find-zipcode [zipcode]
  (mc/find-one-as-map "zipcodes" {:_id (str zipcode)}))

(pprint (find-stations 37.62 -122.37 "metar"))
(pprint (find-zipcode 63111))