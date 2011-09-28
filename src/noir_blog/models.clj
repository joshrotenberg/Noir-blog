(ns noir-blog.models
  (:require [simpledb.core :as db]
            [noir-blog.models.user :as users]
            [noir-blog.models.post :as posts])
  (:use somnium.congomongo)
  (:use [somnium.congomongo.config :only [*mongo-config*]]))


(defn split-mongo-url [url]
  "Parses mongodb url from heroku, eg. mongodb://user:pass@localhost:1234/db"
  ;; Setup the regex.
  (let [matcher (re-matcher #"^.*://(.*?):(.*?)@(.*?):(\d+)/(.*)$" url)] 
    ;; Check if it matches.
    (when (.find matcher)
      ;; Construct an options map.
      (zipmap [:match :user :pass :host :port :db] (re-groups matcher))))) 

(defn maybe-init []
  "Checks if connection and collection exist, otherwise initialize."
  ;; If global connection doesn't exist yet.
  (when (not (connection? *mongo-config*))
    ;; Heroku puts it here.
    (let [mongo-url (get (System/getenv) "MONGOHQ_URL")
          ;; Extract options.
          config  (split-mongo-url mongo-url)] 
      (println "Initializing mongo @ " mongo-url)
      ;; Setup global mongo.
      (mongo! :db (:db config) :host (:host config) :port (Integer. (:port config))) 
      ;; Setup u/p.
      (authenticate (:user config) (:pass config)) 
      ;; Create collection named 'firstcollection' if it doesn't exist.
      (or (collection-exists? :firstcollection)
          (create-collection! :firstcollection)))))

(defn initialize []
  (maybe-init)
  (db/init)
  (when-not (users/get-username "admin")
    (users/init!))
  (posts/init!))
