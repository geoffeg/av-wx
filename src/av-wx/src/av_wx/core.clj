(ns av-wx.core
  (:use [compojure.rotue :only [files not-found]]
        [compojure.handler :only [site]]
        [compojure.core :only [defroutes GET POST ANY context]]
        org.httpkit.server))

(defroutes all-routes
  (GET "/" [] show-index-page))

(run-server (site #'all-routes) {:port 8080})