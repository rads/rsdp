(ns rads.rsdp.modules.exclude-on-timeout
  "Algorithm 2.5: Exclude on Timeout

  Module:
    - Name: PerfectFailureDetector, instance P.

  Events:
    - Indication: ⟨ P, Crash | p ⟩
      - Detects that process p has crashed.

  Properties:
    - PFD1: Strong completeness: Eventually, every process that crashes is
      permanently detected by every correct process.
    - PFD2: Strong accuracy: If a process p is detected by any process, then p
      has crashed."
  (:require
    [rads.rsdp :as rsdp]
    #?@(:clj [[clojure.core.match :refer [match]]]
        :cljs [[cljs.core.match :refer-macros [match]]])))

(defrecord PerfectFailureDetector [id process-id pi alive detected delta]
  rsdp/IModule
  (advance [module event]
    (match [event]
      [[:pfd :init]]
      (-> module
          (assoc :alive pi
                 :detected #{})
          (rsdp/start-timer delta))

      [:timeout]
      (as-> module $
        (reduce (fn [module p]
                  (if (and (not (alive p)) (not (detected p)))
                    (-> module
                        (update :detected conj p)
                        (rsdp/trigger [:pfd :crash p]))
                    (if (not (detected p))
                      (rsdp/trigger module [:pl :send p :heartbeat-request process-id])
                      module)))
                $ (disj pi process-id))
        (assoc $ :alive #{process-id})
        (rsdp/start-timer $ delta))

      [[:pl :deliver process-id :heartbeat-request p]]
      (rsdp/trigger module [:pl :send p :heartbeat-reply process-id])

      [[:pl :deliver process-id :heartbeat-reply p]]
      (update module :alive conj p)

      :else module)))

(defn perfect-failure-detector [& [config]]
  (map->PerfectFailureDetector (merge {:id :pfd} config)))
