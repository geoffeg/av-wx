(ns av-wx.utils
  (:require [clojure.algo.generic.math-functions :as math-f]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader])
  (:use [clojure.pprint]))

(def conf (binding [*read-eval* false]
            (with-open [r (io/reader "/etc/av-wx.conf")]
              (read (PushbackReader. r )))))

(defn cast-map [m types]
  (reduce-kv
   (fn [acc k v]
     (update-in acc [k] #(if-not (clojure.string/blank? %) (v %))))
   m types))

(defn deg2rad [point]
  (mapv #(Math/toRadians %) point))

(defn km2nm [km]
  (* 0.5399568034557 km))

(defn distance-between [point1 point2]
 (let [R 6372.8 ; kilometers
       dlat (Math/toRadians (- (point2 0) (point1 0)))
       dlon (Math/toRadians (- (point2 1) (point1 1)))
       lat1 (Math/toRadians (point1 0))
       lat2 (Math/toRadians (point2 0))
       a (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2))) (* (Math/sin (/ dlon 2)) (Math/sin (/ dlon 2)) (Math/cos lat1) (Math/cos lat2)))]
   (* R 2 (Math/asin (Math/sqrt a)))))

(defn bearing-to [point1 point2]
  (let [[lat1 long1] (deg2rad point1)
        [lat2 long2] (deg2rad point2)
        degrees (Math/toDegrees
     (math-f/atan2
       (* (math-f/sin (- long2 long1)) (math-f/cos lat2))
       (- (* (math-f/cos lat1) (math-f/sin lat2))
          (*
           (math-f/sin lat1)
           (math-f/cos lat2)
           (math-f/cos (- long2 long1))))))]
    (if (< degrees 0) (+ degrees 360) degrees)))

(defn string-to-stream [string]
  (java.io.ByteArrayInputStream.
   (.getBytes (.trim string))))

(defn update-vals [m k f]
  ; update-in m with function f for the keys listed in k
  (reduce #(update-in % [%2] f) m (filter m k)))

;; KJFK to KSFO
;(km2nm (distance-between [40.6397511, -73.7789256] [37.6191050, -122.3752372]))
;(bearing-to ["40.6397511", "-73.7789256"] ["37.6191050", "-122.3752372"])

;; KSTL to KSET
;(km2nm (distance-between [38.7486972, -90.3700289] [38.9296944, -90.4299722]))
;(bearing-to [38.7486972, -90.3700289] [38.9296944, -90.4299722])
