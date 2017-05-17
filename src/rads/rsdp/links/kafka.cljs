(ns rads.rsdp.links.kafka
  (:require
    [cljs.core.async :as async]
    [cljs-http.client :as http]
    [cljs.reader :as reader]
    [goog.crypt.base64 :as base64]
    [rads.rsdp :as rsdp]))

(def ^:private current-version 72)

(defn- publish-url [{:keys [version] :as links}]
  (str "http://localhost:8082/topics/t" version))

(defn- poll-url [{:keys [process-id version] :as links}]
  (str "http://localhost:8082/consumers/" (name process-id)
       "/instances/i" version "/records"))

(defn- json->clj [json-str]
  (-> (js/JSON.parse json-str)
      (js->clj :keywordize-keys true)))

(defn- debug [x]
  (println "sending:" x)
  x)

(defn- encode [value]
  (-> value
      (update :modules #(mapv (partial into {}) %))
      (debug)
      (pr-str)
      (base64/encodeString)))

(defn- decode [value]
  (-> value
      (base64/decodeString)
      (reader/read-string)))

(defn- parse [record]
  (->> record
       (json->clj)
       (map (comp decode :value))))

(defrecord PerfectLinks [id process-id version]
  rsdp/ILinks
  (poll! [links]
    (let [ch (async/chan 1 (map #(update % :body parse)))]
      (http/request
       {:method :get
        :url (poll-url links)
        :channel ch})))
  (publish! [links events]
    (let [ch (async/chan 1 (map (comp json->clj :body)))
          records (map (fn [e] {:value (encode e)})
                       events)]
      (http/request
       {:method :post
        :url (publish-url links)
        :headers {"Content-Type" "application/vnd.kafka.binary.v2+json"
                  "Accept" "application/vnd.kafka.v2+json"}
        :json-params {:records records}
        :channel ch}))))

(defn perfect-links []
  (map->PerfectLinks
   {:id :pl
    :version current-version}))
