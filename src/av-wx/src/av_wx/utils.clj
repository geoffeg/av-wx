(ns av-wx.reports
  (:require [clojure.algo.generic.math-functions :as math-f]))

(defn deg2rad [point]
  (mapv #(Math/toRadians %) point))

(defn distance-between [point1 point2]
  (let [[lat1 long1] (deg2rad point1)
        [lat2 long2] (deg2rad point2)]
    (* 3956.09 (math-f/acos
        (+
         (*
          (math-f/sin lat1)
          (math-f/sin lat2))
         (*
          (math-f/cos lat1)
          (math-f/cos lat2)
          (-
           (math-f/cos long2)
           (math-f/cos long1))))))))

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

(distance-between [49.2000 -98.1000] [35.9939, -78.8989])
(bearing-to [49.2000 -98.1000] [35.9939, -78.8989])