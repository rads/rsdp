(ns rads.rsdp.modules.eliminate-duplicates
  "Algorithm 2.2: Eliminate Duplicates

  Module:
    - Name: PerfectPointToPointLinks, instance pl.

  Events:
    - Request: ⟨ pl, Send | q, m ⟩
      - Requests to send message m to process q.
    - Indication: ⟨ pl, Deliver | p, m ⟩
      - Delivers message m sent by process p.

  Properties:
    - PL1: Reliable delivery
      - If a correct process p sends a message m to a correct process q, then
        q eventually delivers m.
    - PL2: No duplication
      - No message is delivered by a process more than once.
    - PL3: No creation
      - If some process q delivers a message m with sender p, then m was
        previously sent to q by process p."
  (:require
    [rads.rsdp :as rsdp]
    #?@(:clj [[clojure.core.match :refer [match]]]
        :cljs [[cljs.core.match :refer-macros [match]]])))

(defrecord PerfectLinks [process-ids delivered]
  rsdp/IModule
  (advance [module event]
    (let [{:keys [pl sl]} process-ids]
      (match [event]
        [[pl :init]] (assoc module :delivered #{})
        [[pl :send q m]] (rsdp/trigger module [sl :send q m])
        [[sl :deliver p m]] (if (delivered m)
                              module
                              (-> module
                                  (update :delivered conj m)
                                  (rsdp/trigger [pl :deliver p m])))
        :else :no-match))))

(defn perfect-links [& [config]]
  (map->PerfectLinks config))
