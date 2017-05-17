(ns rads.rsdp.browser.components.timeline
  (:require
    [goog.functions]
    [reagent.core :as reagent]))

(defn line-x [n]
  (+ 100 (* n 100)))

(defn line-start [n]
  [(line-x n) 72])

(defn line-end [n]
  [(line-x n) 10000])

(defn process-line [{:keys [n] :as props}]
  (let [start (line-start n)
        end (line-end n)]
   [:line.Timeline-process-line
    {:x1 (str (first start)), :y1 (str (second start))
     :x2 (str (first end)), :y2 (str (second end))
     :stroke-width "4"
     :stroke "#ccc"}]))

(defn step-y [line-y step-n]
  (+ line-y (* step-n 15) -15))

(defn step-marker [{:keys [process-n step-n step] :as props}]
  (let [[x y] (line-start process-n)]
    [:circle.Timeline-step-marker
     {:cx x
      :cy (step-y y step-n)
      :r 5
      :fill "#428ade"}]))

(defn process-n [pi step]
  (get (zipmap pi (range)) (:process-id step)))

(defn other-step-n [prev-step reversed-log]
  (some (fn [[n step]]
          (when (and (= (:logical-clock step) (:logical-clock prev-step))
                     (= (:process-id step) (:process-id prev-step)))
            n))
        (map-indexed vector reversed-log)))

(defn delivery-marker [{:keys [pi step-n cur-step reversed-log] :as props}]
  (let [{:keys [delivery]} cur-step
        prev-step (:from delivery)
        _ (println "delivery:" delivery)
        [x1 y1] (line-start (process-n pi cur-step))
        [x2 y2] (line-start (process-n pi prev-step))]
    [:line.Timeline-delivery-marker
     {;; (x1,y1) is the source step
      ;; (x2,y2) is the destination step
      :x1 x1 :y1 (step-y y1 step-n)
      :x2 x2 :y2 (step-y y2 (other-step-n prev-step reversed-log))
      :stroke-width "2"
      :stroke "#ccc"}]))

(defn timeline [{:keys [process log] :as props}]
  (let [reversed-log (rseq (or log []))
        {:keys [pi]} process
        sorted-pi (sort pi)]
    [:div.Timeline
     [:svg
      (map-indexed (fn [n _]
                     [process-line {:n n :key n}])
                   sorted-pi)
      (map-indexed (fn [n step]
                     [step-marker {:key n
                                   :process-n (process-n sorted-pi step)
                                   :pi sorted-pi
                                   :step-n n
                                   :step step}])
                   reversed-log)
      (->> reversed-log
           (map-indexed vector)
           (filter (comp :from :delivery second))
           (map (fn [[n step]]
                  [delivery-marker {:key n
                                    :process process
                                    :pi sorted-pi
                                    :step-n n
                                    :cur-step step
                                    :reversed-log reversed-log}])))]]))