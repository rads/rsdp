(ns rads.rsdp.modules.synchronous-job-handler
  "Algorithm 1.1: Synchronous Job Handler
  
  Module:
    - Name: JobHandler, instance jh.

  Events:
    - Request: ⟨ jh, Submit | job ⟩
      - Requests a job to be processed.
    - Indication: ⟨ jh, Confirm | job ⟩
      - Confirms that the given job has been (or will be) processed.

  Properties:
    - JH1: Guaranteed response
      - Every submitted job is eventually confirmed."
  (:require
    [clojure.core.match :refer [match]]
    [rads.rsdp :as rsdp]))

(defrecord JobHandler [process-ids job-processor]
  rsdp/IModule
  (advance [module event]
    (let [{:keys [jh]} process-ids]
      (match [event]
        [[jh :submit job]] (do
                             (job-processor job)
                             (rsdp/trigger module [jh :confirm job]))
        :else :no-match))))

(defn job-handler [config]
  (map->JobHandler config))
