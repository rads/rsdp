(ns rads.rsdp.modules.fair-loss-links
  "Module:
    Name: FairLossPointToPointLinks, instance fll.

  Events:
    - Request: ⟨ fll, Send | q, m ⟩
      - Requests to send message m to process q.
    - Indication: ⟨ fll, Deliver | p, m ⟩
      - Delivers message m sent by process p.

  Properties:
    - FLL1: Fair-loss
      - If a correct process p infinitely often sends a message m to a correct
        process q, then q delivers m an infinite number of times.
    - FLL2: Finite duplication
      - If a correct process p sends a message m a finite number of times to
        process q, then m cannot be delivered an infinite number of times by q.
    - FLL3: No creation
      - If some process q delivers a message m with sender p, then m was
        previously sent to q by process p."
  (:require
    [aleph.udp :as udp]
    [byte-streams]
    [clojure.core.async :as async :refer [<!!]]
    [clojure.edn :as edn]
    [manifold.stream :as stream]
    [rads.rsdp :as rsdp]))

;; Fair Loss Links using UDP

(defrecord FairLossLinks [server client peers in errors port]
  rsdp/ILinks
  (-start! [links]
    (let [s (udp/socket {:port port})
          c (udp/socket {})]
      (stream/consume #(async/put! in %) @s)
      (assoc links :server s :client c)))
  (-stop! [links]
    (.close @server)
    (.close @client)
    (dissoc links :server :client))
  (-put! [_ event]
    (when-let [peer (get peers (rsdp/destination event))]
      (let [{:keys [host port]} peer
            packet {:host host
                    :port port
                    :message (pr-str event)}]
        (stream/put! @client packet))))
  (-take! [_]
    (<!! in)))

(def decode-xf
  (map (comp edn/read-string
             byte-streams/to-string
             :message)))

(defn fair-loss-links [{:keys [in port peers] :as config}]
  (map->FairLossLinks
    {:port port
     :peers peers
     :in (or in (async/chan 100 decode-xf))}))
