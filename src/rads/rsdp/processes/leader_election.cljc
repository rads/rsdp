(ns rads.rsdp.processes.leader-election
  (:require
    [rads.rsdp.modules.exclude-on-timeout :refer [perfect-failure-detector]]
    [rads.rsdp.modules.monarchical-leader-election :refer [leader-election]]
    [rads.rsdp.links.kafka :refer [perfect-links]]
    [rads.rsdp.process :as process]
    #?@(:clj [[clojure.core.async :as async]]
        :cljs [[cljs.core.async :as async]])))

(defn leader-election-process [{:keys [id pi delta channels] :as config}]
  (process/process
   {:id id
    :pi pi
    :modules [(leader-election {:pi pi})
              (perfect-failure-detector {:pi pi :delta delta})
              (perfect-links)]
    :channels channels}))

