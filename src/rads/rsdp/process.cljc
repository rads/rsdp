(ns rads.rsdp.process
  (:require
    [rads.rsdp :as rsdp]
    [rads.rsdp.evaluator :as evaluator]
   #?@(:clj [[clojure.core.async :as async :refer [<! >! alts! go go-loop]]]
       :cljs [[cljs.core.async :as async :refer [<! >! alts!]]]))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]])))

(defn process [config]
  config)

(defn- accepted? [id event]
  (and (= :pl (nth event 0))
       (= id (nth event 2))))

(defn- tick-all [evaluator modules links process-id steps]
  (let [bottom-id (:id (last modules))]
    (reduce (fn [ev step]
              (let [events (->> (:modules step)
                                (mapcat :out)
                                (filter #(accepted? process-id %))
                                (map #(assoc % 1 :deliver)))]
                (reduce #(evaluator/tick %1 {:from step :event %2} bottom-id)
                        ev
                        events)))
            evaluator
            steps)))

(defn- handle-incoming-steps [ev modules links id body out]
  (go
    (let [updated (tick-all ev modules links id body)]
      (async/onto-chan out body false)
      (when-let [new-steps (evaluator/diff-steps ev updated)]
        (<! (rsdp/publish! links new-steps)))
      updated)))

(defn- handle-error [errors response stop]
  (async/close! stop)
  (async/put! errors (ex-info "Invalid status... shutting down."
                              {:response response})))

(defn- start-timeout! [modules timeouts]
  (go
    (<! (async/timeout (-> modules last :delta)))
    (>! timeouts true)))

(defn- handle-timeout [ev links modules timeouts]
  (go
    (let [updated (evaluator/tick ev {:event :timeout} :pfd)
          new-steps (evaluator/diff-steps ev updated)]
      (<! (rsdp/publish! links new-steps))
      (start-timeout! modules timeouts)
      updated)))

(defn- poll-links [ev modules links id out stop errors]
  (go
    (let [response (<! (rsdp/poll! links))
          {:keys [status body]} response]
      (cond
        (not= status 200) (handle-error errors response stop)
        (not (seq body)) ev
        true (<! (handle-incoming-steps ev modules links id body out))))))

(defn start! [{:keys [id modules channels] :as process}]
  (let [{:keys [in out stop errors]} channels
        modules (evaluator/init-modules id modules)
        links (last modules)
        modules (into [] (butlast modules))
        evaluator (evaluator/evaluator id modules)
        initialized (evaluator/init evaluator)
        timeouts (async/chan 1)]
    (go
      (start-timeout! modules timeouts)
      (<! (rsdp/publish! links (:log initialized)))
      (loop [ev initialized]
        (let [[_ ch] (alts! [stop timeouts] :priority true :default nil)]
          (condp = ch
            stop (println (str "Stop event received. Shutting down process" id "."))
            timeouts (recur (<! (handle-timeout ev links modules timeouts)))
            :default (recur (<! (poll-links ev modules links id out stop errors)))))))))
