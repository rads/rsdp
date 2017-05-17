(ns rads.rsdp.modules.job-transformation-by-buffering
  "Algorithm 1.3: Job-Transformation by Buffering
  
  Module:
    - Name: TransformationHandler, instance th.

  Events:
    - Request: ⟨ th, Submit | job ⟩
      - Submits a job for transformation and for processing.
    - Indication: ⟨ th, Confirm | job ⟩
      - Confirms that the given job has been (or will be) transformed and processed.
    - Indication: ⟨ th, Error | job ⟩
      - Indicates that the transformation of the given job failed.

  Properties:
    - TH1: Guaranteed response: Every submitted job is eventually confirmed or its
           transformation fails.
    - TH2: Soundness: A submitted job whose transformation fails is not processed.")

(defn advance [state event {:keys [th jh M] :as config}]
  (let [{:keys [::top ::bottom ::handling ::buffer]} state]
    (match [event]
      [[th :init]] {::top 1
                    ::bottom 1
                    ::handling false
                    ::buffer (into [] (take M (repeat nil)))}
      [[th :submit job]] (if (= top (+ bottom M))
                           (assoc state :trigger [[th :error job]])
                           (-> state
                               (assoc-in [::buffer (mod top (inc M))] job)
                               (update ::top inc)
                               (assoc :trigger [[th :confirm job]])))
      [:upon] (when (and (< bottom top) (not handling))
                (let [job (get buffer (mod bottom (inc M)))]
                  (-> state
                      (update ::bottom inc)
                      (assoc ::handling true)
                      (assoc :trigger [[jh :submit job]]))))
      [[jh :confirm job]] (assoc state ::handling false)
      :else nil)))
