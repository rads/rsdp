(ns rads.rsdp.modules.sequence-number
  "Algorithm 2.11: Sequence Number
  
  Module:
    - Name: FIFOPerfectPointToPointLinks, instance fpl.

  Events:
    - Request: ⟨ fpl, Send | q, m ⟩
      - Requests to send message m to process q.
    - Indication: ⟨ fpl, Deliver | p, m ⟩
      - Delivers message m sent by process p.

  Properties:
    - FPL1–FPL3:
      - Same as properties PL1–PL3 of perfect point-to-point links
        (Module 2.3).
    - FPL4: FIFO delivery
      - If some process sends message m1 before it sends message m2, then no
        correct process delivers m2 unless it has already delivered m1."
  (:require
    [aleph.tcp :as tcp]
    [clojure.core.async :as async :refer [<!!]]
    [clojure.edn :as edn]
    [gloss.core :as gloss]
    [gloss.io :as io]
    [manifold.deferred :as deferred]
    [manifold.stream :as stream]
    [rads.rsdp :as rsdp]))

;; Perfect Links using TCP

(def protocol
  (gloss/compile-frame
    (gloss/finite-frame :uint32
      (gloss/string :utf-8))
    pr-str
    edn/read-string))

(defn- wrap-duplex-stream [protocol s]
  (let [out (stream/stream)]
    (stream/connect
      (stream/map #(io/encode protocol %) out)
      s)
    (stream/splice
      out
      (io/decode-stream s protocol))))

(defn- tcp-client [host port]
  (deferred/chain
    (tcp/client {:host host, :port port})
    #(wrap-duplex-stream protocol %)))

(defn- start-tcp-server [handler port]
  (tcp/start-server
    (fn [s info]
      (handler (wrap-duplex-stream protocol s) info))
    {:port port}))

(defrecord PerfectLinks [server peers in port]
  rsdp/ILinks
  (-start! [links]
    (let [handler (fn [s info]
                    (stream/connect s in))
          new-server (start-tcp-server handler port)
          peers-xf (map (fn [[k v]] 
                          (let [c (delay @(tcp-client (:host v) (:port v)))]
                            [k (assoc v :client c)])))
          new-peers (into {} peers-xf peers)]
      (assoc links
             :server new-server
             :peers new-peers)))
  (-stop! [links]
    (.close server)
    (dissoc links :server))
  (-put! [links event]
    (when-let [peer (get peers (rsdp/destination event))]
      (let [{:keys [client]} peer]
        (stream/put! @client event))))
  (-take! [links]
    (<!! in)))

(defn perfect-links [{:keys [in port peers] :as config}]
  (map->PerfectLinks
    {:port port
     :peers peers
     :in (or in (async/chan 100))})) 
