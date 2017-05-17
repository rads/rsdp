(ns rads.rsdp.browser.components.log-viewer
  (:require
    [reagent.core :as reagent]
    [cljs.core.async :as async]))

(defn on-row-click [updates offset event]
  (async/put! updates {:action :seek
                       :offset offset}))

(defn row [updates total index step]
  (let [{:keys [process-id action comment]} step
        offset (- total index)]
    [:tr {:key offset
          :on-click (partial on-row-click updates offset)}
     [:td offset]
     [:td process-id]
     [:td (str action)]
     [:td comment]]))

(defn log-viewer [{:keys [log updates] :as props}]
  [:div.LogViewer
   [:table
    [:thead
     [:tr
      [:td "\uD83D\uDD52"]
      [:td "Process"]
      [:td "Module"]
      [:td "Action"]
      [:td ""]]]
    [:tbody
     (map-indexed (partial row updates (count log))
                  (rseq (or log [])))]]])
