(ns rads.rsdp.modules.monarchical-leader-election
  "Algorithm 2.6: Monarchical Leader Election

  Module:
    - Name: LeaderElection, instance le.
  
  Events:
    - Indication: ⟨ le, Leader | p ⟩
      - Indicates that process p is elected as leader.

  Properties:
    - LE1: Eventual detection
      - Either there is no correct process, or some correct process is
        eventually elected as the leader.
    - LE2: Accuracy
      - If a process is leader, then all previously elected leaders have
        crashed."
  (:require
    [rads.rsdp :as rsdp]
    [clojure.set :as set]
    #?@(:clj [[clojure.core.match :refer [match]]]
        :cljs [[cljs.core.match :refer-macros [match]]])))

(def ^:private lowest-rank nil)

(defn- max-rank [processes]
  (first (sort processes)))

(defrecord LeaderElection [id pi suspected leader]
  rsdp/IModule
  (advance [module event]
    (match [event]
      [[:le :init]] (assoc module
                           :suspected #{}
                           :leader lowest-rank)
      [[:pfd :crash p]] (update module :suspected conj p)
      [:upon] (let [local-max (max-rank (set/difference pi suspected))]
                (if (= leader local-max)
                  module
                  (-> module
                      (assoc :leader local-max)
                      (rsdp/trigger [:le :leader local-max]))))
      :else module)))

(defn leader-election [& [config]]
  (map->LeaderElection (merge {:id :le} config)))
