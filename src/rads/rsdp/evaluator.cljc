(ns rads.rsdp.evaluator
  (:require
    [rads.rsdp :as rsdp]
    [clojure.test.check.generators :as gen]
    #?@(:clj [[clojure.spec :as s]]
        :cljs [[cljs.spec :as s]])))

;; Specs

(s/def ::process-id keyword?)

(s/def ::event-name keyword?)
(s/def ::event (s/or :module-event vector?
                     :upon-event #{:upon}
                     :timeout-event #{:timeout}))

(s/def ::id keyword?)
(s/def ::in (s/coll-of ::event))
(s/def ::out (s/coll-of ::event))
(s/def ::module (s/keys :opt-un [::id ::in ::out]))

(s/def ::modules (s/coll-of ::module :min-count 1 :kind vector?))
(s/def ::logical-clock nat-int?)
(s/def ::wall-clock nat-int?)
(s/def ::comment string?)
(s/def ::action #{:receive :init :deliver :compute})
(s/def ::step (s/keys :req-un [::process-id
                               ::modules
                               ::logical-clock
                               ::wall-clock
                               ::action
                               ::comment]
                      :opt-un [::delivery]))
(s/def ::log (s/coll-of ::step :min-count 1 :kind vector?))
(s/def ::evaluator (s/keys :req-un [::log]))

(s/def ::delivery (s/keys :req-un [::event] :opt-un [::from]))
(s/def ::next-fields (s/keys :req-un [::action ::comment]
                             :opt-un [::delivery]))

;; Private Helpers

(defn- destination [event]
  (first event))

(defn- wall-clock []
  #?(:clj (System/currentTimeMillis)
     :cljs (js/Date.now)))

(defn- update-module [step module-id f & xs]
  (update step :modules
          (fn [modules]
            (mapv (fn [{:keys [id] :as module}]
                    (if (= id module-id)
                      (apply f module xs)
                      module))
                  modules))))

(defn- get-module [{:keys [modules] :as step} module-id]
  (first (filter #(= (:id %) module-id) modules)))

(defn- reset-module [module]
  (assoc module
    :out []
    :timers []))

(defn- next-step [prev-step next-fields]
  (-> prev-step
      (update :modules #(mapv reset-module %))
      (update :logical-clock inc)
      (assoc :out []
             :wall-clock (wall-clock))
      (dissoc :delivery)
      (merge (select-keys next-fields [:action :comment :delivery]))))

(s/fdef next-step
  :args (s/cat :prev-step ::step
               :next-fields ::next-fields))

(defn- run-receive-step [prev-step {:keys [event] :as delivery} destination-id]
  (-> (next-step prev-step {:action :receive
                            :comment (str "Receive " (pr-str event) " from " destination-id)
                            :delivery delivery})
      (update-module destination-id #(update % :in conj event))))

(defn- local-event? [event]
  (not= (destination event) :pl))

(defn- modules-with-outgoing-local-events [{:keys [modules] :as step}]
  (->> modules
       (map (fn [{:keys [id] :as module}]
              (update module :out #(filter local-event? %))))
       (remove (comp empty? :out))))

(defn- add-input [step module-id event]
  (update-module step module-id
                 (fn [module]
                   (update module :in conj event))))

(defn- remove-output [step module-id event]
  (update-module step module-id
                 (fn [module]
                   (let [xf (remove #(= % event))]
                     (update module :out #(into [] xf %))))))

(defn- module-above [module-id modules]
  (->> modules
       (take-while #(not= (:id %) module-id))
       (last)))

(defn- run-one-delivery-step [{:keys [modules] :as prev-step} module-id]
  (let [module (get-module prev-step module-id)
        event (->> (:out module)
                   (filter local-event?)
                   (first))
        destination-id (destination event)
        topmost-module-id (-> prev-step :modules first :id)
        above-id (:id (module-above module-id modules))
        next-fields {:action :deliver
                     :comment (str "Delivering from " module-id " to " destination-id)}]
    (cond
      (= destination-id topmost-module-id)
      (-> (next-step prev-step next-fields)
          (update :out conj event)
          (remove-output module-id event))

      (= destination-id module-id)
      (-> (next-step prev-step next-fields)
          (add-input above-id event)
          (remove-output module-id event))

      true
      (-> (next-step prev-step next-fields)
          (add-input destination-id event)
          (remove-output module-id event)))))

(defn- run-all-delivery-steps [prev-step]
  (loop [log []]
    (let [current-step (or (last log) prev-step)
          outgoing (modules-with-outgoing-local-events current-step)]
      (if-not (seq outgoing)
        log
        (let [module (first outgoing)
              delivery-step (run-one-delivery-step current-step (:id module))]
          (recur (conj log delivery-step)))))))

(defn- modules-with-remaining-computations [{:keys [modules] :as step}]
  (filter #(not-empty (:in %)) modules))

(defn- run-one-computation-step [prev-step module-id]
  (-> (next-step prev-step {:action :compute
                            :comment (str "Processing input in " module-id)})
      (update-module module-id
                     (fn [{:keys [in] :as module}]
                       (-> module
                           (rsdp/advance (peek in))
                           (update :in pop))))))

(defn- run-all-computation-steps [prev-step]
  (loop [log []]
    (let [current-step (or (last log) prev-step)
          remaining (modules-with-remaining-computations current-step)]
      (if-not (seq remaining)
        log
        (let [module (first remaining)
              computation-step (run-one-computation-step current-step (:id module))
              delivery-steps (run-all-delivery-steps computation-step)
              new-steps (cons computation-step delivery-steps)]
          (recur (into log new-steps)))))))


(defn- run-steps [{:keys [log] :as evaluator} delivery destination-id]
  (let [prev-step (last log)
        receive-step (run-receive-step prev-step delivery destination-id)
        computation-steps (run-all-computation-steps receive-step)
        new-steps (cons receive-step computation-steps)]
    (update evaluator :log into new-steps)))

;; Public API

(defn init-modules [process-id modules]
  (let [initial-state {:in []
                       :out []
                       :timers []
                       :process-id process-id}]
    (mapv #(merge % initial-state) modules)))

(defn evaluator [process-id modules]
  {:log [{:comment "Initialized module"
          :action :init
          :process-id process-id
          :logical-clock 0
          :wall-clock (wall-clock)
          :out []
          :modules modules}]})

(s/fdef evaluator
  :args (s/cat :process-id ::process-id
               :modules ::modules)
  :ret ::evaluator)

(defn tick
  ([{:keys [log] :as evaluator} delivery]
   (tick evaluator delivery (-> (last log) :modules first :id)))
  ([{:keys [log] :as evaluator} delivery destination-id]
   (let [prev-step (last log)
         {:keys [modules]} prev-step]
     (as-> evaluator $
       (run-steps $ delivery destination-id)
       (reduce #(run-steps %1 {:event :upon} %2) $ (map :id modules))))))

(s/fdef tick
  :args (s/alt :no-destination (s/cat :evaluator ::evaluator
                                        :delivery ::delivery)
               :with-destination (s/cat :evaluator ::evaluator
                                        :delivery ::delivery
                                        :destination-id ::id))
  :ret ::evaluator)

(defn init [{:keys [log] :as evaluator}]
  (let [{:keys [modules]} (last log)
        init-events (map (fn [{:keys [id]}]
                           [id :init])
                         modules)]
    (as-> evaluator $
     (reduce #(run-steps %1 {:event %2} (destination %2)) $ init-events)
     (reduce #(run-steps %1 {:event :upon} %2) $ (map :id modules)))))

(s/fdef init
  :args (s/cat :evaluator ::evaluator)
  :ret ::evaluator)

(defn diff-steps [ev1 ev2]
  (let [prev-offset (count (:log ev1))]
    (seq (subvec (:log ev2) prev-offset))))

(s/fdef diff-steps
  :args (s/cat :ev1 ::evaluator
               :ev2 ::evaluator)
  :ret ::log)
