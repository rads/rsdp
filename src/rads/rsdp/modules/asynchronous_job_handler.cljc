(ns rads.rsdp.modules.asynchronous-job-handler
  "Algorithm 1.2: Asynchronous Job Handler
  
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

(defrecord JobHandler [process-ids buffer job-processor select-job]
  rsdp/IModule
  (advance [process event]
    (let [{:keys [jh]} process-ids]
      (match [event]
        [[jh :init]] (assoc process :buffer #{})
        [[jh :submit job]] (-> process
                               (update :buffer conj job)
                               (rsdp/trigger [jh :confirm job]))
        [:upon] (when (seq (:buffer process))
                  (let [job (select-job (:buffer process))]
                    (job-processor job)
                    (update process :buffer disj job)))
        :else :no-match))))

(defn job-handler [config]
  (map->JobHandler config))
