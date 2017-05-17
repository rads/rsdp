(ns rads.rsdp.modules.retransmit-forever
  "Algorithm 2.1: Retransmit Forever

  Module:
    - Name: StubbornPointToPointLinks, instance sl.

  Events:
    - Request: ⟨ sl, Send | q, m ⟩
      - Requests to send message m to process q.
    - Indication: ⟨ sl, Deliver | p, m ⟩
      - Delivers message m sent by process p.

  Properties:
    - SL1: Stubborn delivery
      - If a correct process p sends a message m once to a correct process q,
        then q delivers m an infinite number of times.
    - SL2: No creation
      - If some process q delivers a message m with sender p, then m was
        previously sent to q by process p."

  (:require
    [rads.rsdp :as rsdp]
    #?@(:clj [[clojure.core.match :refer [match]]]
        :cljs [[cljs.core.match :refer-macros [match]]])))

(defrecord StubbornLinks [process-ids sent delta]
  rsdp/IModule
  (advance [module event]
    (let [{:keys [sl fll]} process-ids]
      (match [event]
        [[sl :init]] (-> module
                         (assoc :sent #{})
                         (rsdp/start-timer delta))
        [[:timeout]] (as-> module $
                           (reduce (fn [p [q m]]
                                     (rsdp/trigger p [fll :send q m]))
                                   $ (:sent module))
                           (rsdp/start-timer $ delta))
        [[sl :send q m]] (-> module
                             (rsdp/trigger [fll :send q m])
                             (update :sent conj [q m]))
        [[fll :deliver p m]] (rsdp/trigger module [sl :deliver p m])
        :else :no-match))))

(defn stubborn-links [& [config]]
  (map->StubbornLinks config))
