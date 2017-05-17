(ns rads.rsdp.browser
  (:require
    [cljs.core.async :as async :refer [<! >! alts!]]
    [com.stuartsierra.component :as component]
    [goog.dom :as gdom]
    [rads.rsdp.browser.components.root :as root]
    [rads.rsdp.process :as process]
    [rads.rsdp.processes.leader-election :refer [leader-election-process]]
    [reagent.core :as reagent]
    [cljs.spec.test :as stest])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [cljs.core.match :refer [match]]))

;; Private Helpers

(defn- new-process [id]
  (leader-election-process
   {:id id
    :pi #{:a :b :c :d :e}
    :delta 10000
    :channels {:in (async/chan 100)
               :out (async/chan 100)
               :errors (async/chan 100)
               :stop (async/chan)}}))

(defn- initial-state [{:keys [pi] :as process}]
  {:process process
   :process-logs (into {} (map (fn [p] [p []]) pi))
   :log []})

(defn seek [{:keys [state history] :as browser} offset]
  (reset! state (get @history offset)))

(defn- handle-ui-update [browser event]
  (match [event]
    [{:action :seek :offset offset}] (seek browser offset)))

(defn- start-process-consumer! [{:keys [history process] :as browser}]
  (let [{:keys [channels]} process
        {:keys [errors stop out]} channels]
    (go-loop []
      (let [[val ch] (alts! [errors stop out] :priority true)]
        (condp = ch
          stop (println "Stopping UI consumer...")
          errors (do
                   (println "ERROR:" (pr-str val))
                   (recur))
          out (do
                (swap! history
                       (fn [h]
                         (conj h (-> (last h)
                                     (update :log conj val)
                                     (update-in [:process-logs (:process-id val)] conj val)))))
                (recur)))))))

(defn- start-ui-consumer! [{:keys [updates state history] :as browser}]
  (let [stop (async/chan)]
    (go-loop []
      (let [[event ch] (alts! [stop updates] :priority true)]
        (when (not= ch stop)
          (handle-ui-update browser event)
          (recur))))
    stop))

(defrecord ^:private Browser [updates state history ui-consumer process]
  component/Lifecycle
  (start [browser]
    (add-watch history :state
               (fn [_ _ _ new-state]
                 (reset! state (last new-state))))
    (assoc browser :ui-consumer (start-ui-consumer! browser)))
  (stop [browser]
    (remove-watch history :state)
    (async/close! ui-consumer)
    (assoc browser :ui-consumer nil)))

;; Public API

(defn browser []
  (map->Browser
    {:updates (async/chan 100)
     :state (reagent/atom {})
     :history (atom [])
     :ui-consumer nil
     :process nil}))

(defn start-process! [{:keys [history] :as browser} process-id]
  (let [process (new-process process-id)
        new-browser (assoc browser :process process)]
    (reset! history [(initial-state process)])
    (start-process-consumer! new-browser)
    (process/start! process)
    new-browser))

(defn stop-process! [{:keys [process] :as browser} process-id]
  (async/close! (get-in process [:channels :stop]))
  (assoc browser :process nil))

;; Initialization

(defonce b (atom (browser)))

(defn ^:export start [id-string]
  (swap! b start-process! (keyword id-string)))

(def ^:private container (gdom/getElement "Container"))

(defn- render [browser]
  (let [props (select-keys browser [:state :updates])]
    (reagent/render [root/root props] container)))

(defn on-jsload []
  (render @b))

(defn- enable-dev! []
  (enable-console-print!)
  (println "+ (enable-console-print!)")
  (stest/instrument)
  (println "+ (stest/instrument)"))

(defn -main [& args]
  (enable-dev!)
  (render (swap! b component/start)))
