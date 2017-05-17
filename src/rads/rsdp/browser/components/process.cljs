(ns rads.rsdp.browser.components.process
  (:require
    [goog.functions]
    [cljs.pprint :refer [pprint]]
    [reagent.core :as reagent]))

(defn module [{:keys [title state] :as props}]
  [:div.Process-module
   [:h3.Process-module-title title]
   [:ul.Process-module-state [:pre (with-out-str (pprint state))]]])

(defn process [props]
  (let [{:keys [process-logs]} props
        {:keys [id] :as proc} (:process props)
        log (get process-logs id)
        modules (get (last log) :modules)]
    [:div.Process
     [:div.Process-id
      [:span.Process-id-subtitle "Process"]
      [:h2.Process-id-header (str id)]]
     [:div.Process-modules
      (for [m modules]
        [module {:key (:id m)
                 :title (:id m)
                 :state m}])]]))
