(ns rads.rsdp.browser.components.root
  (:require
    [reagent.core :as reagent]
    [rads.rsdp.browser.components.network :as network]
    [rads.rsdp.browser.components.timeline :as timeline]
    [rads.rsdp.browser.components.process :as process]
    [rads.rsdp.browser.components.log-viewer :as log-viewer]))

(defn root [{:keys [state updates] :as props}]
  (let [{:keys [log process process-logs]} @state]
    [:div.Root
     [:div.LeftColumn
      [network/network {:process process}]
      [timeline/timeline {:log log :process process}]]
     [:div.RightColumn
      [process/process {:process process :process-logs process-logs}]
      [log-viewer/log-viewer {:log log :updates updates}]]]))
