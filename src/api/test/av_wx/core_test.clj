(ns av-wx.core-test
  (:require [av-wx.db :as db]
            [av-wx.utils :as utils])
  (:use clojure.test
        av-wx.core))

(deftest find-stations
  (testing "MongoDB search for station by lat lon failed"
    (is (= (get-in (db/find-stations 37.62 -122.37 "metar") ["obj" "icao"]) "KSFO" ))))

(deftest find-zipcode
  (testing "MongoDB search for zipcode lat/lon failed"
    (is (= (db/find-zipcode 63111) [-90.2495 38.5633]))))

(deftest distance-between
  (testing "Distance between"
    (testing "KSTL and KSET" ; http://www.gcmap.com/dist?P=KSTL-KSET&DU=km&DM=&SG=&SU=mph
      (is (= (utils/distance-between [38.7486972, -90.3700289] [38.9296944, -90.4299722]) 20.790691704285102)))
    (testing "KJFK and KSFO" ; http://www.gcmap.com/dist?P=KJFK-KSFO&DU=km&SU=mph
      (is (= (utils/distance-between [40.6397511, -73.7789256] [37.6191050, -122.3752372]) 4152.97383569412)))))

(deftest km2nm
  (testing "Kilometers to nautical miles"
    (is (= (utils/km2nm 500) 269.97840172785))
    (is (= (utils/km2nm 10.5) 5.6695464362848496))
    (is (= (utils/km2nm 0.5) 0.26997840172785))))

(deftest bearing-to
  (testing "Bearing"
    (testing "from KSTL to KSET" ; http://www.gcmap.com/dist?P=KSTL-KSET&DU=km&DM=&SG=&SU=mph
      (is (= (int (utils/bearing-to [38.7486972, -90.3700289] [38.9296944, -90.4299722])) 345)))
    (testing "from KJFK to KSFO" ; http://www.gcmap.com/dist?P=KJFK-KSFO&DU=km&SU=mph
      (is (= (int (utils/bearing-to [40.6397511, -73.7789256] [37.6191050, -122.3752372])), 281)))))