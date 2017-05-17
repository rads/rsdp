(ns rads.rsdp.browser.components.network
  (:require
    [goog.functions]
    [reagent.core :as reagent]))

(defn degrees->radians [angle]
  (* angle (/ js/Math.PI 180)))

(defn node [{:keys [bounds n total] :as props}]
  (let [{:keys [width height]} bounds
        center {:x (/ width 2)
                :y (/ height 2)}
        angle (degrees->radians (+ 30 (* n (/ 360 total))))
        r (min (/ height 3) (/ width 3))]
    [:circle {:cx (+ (:x center) (* r (js/Math.cos angle)))
              :cy (+ (:y center) (* r (js/Math.sin angle)))
              :r 20
              :fill (nth (repeat "#222") n)}]))

(defn get-bounds [node]
  {:width (.-clientWidth node)
   :height (.-clientHeight node)})

(defn network []
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (let [debounce-interval-ms 200
            set-bounds (js/goog.functions.debounce
                        (fn [& _]
                          (let [node (reagent/dom-node this)
                                bounds (get-bounds node)]
                            (reagent/set-state this {:bounds bounds})))
                        debounce-interval-ms)]
        (.addEventListener js/window "resize" set-bounds)
        (set-bounds)
        (reagent/set-state this {:listener set-bounds})))
    :component-will-unmount
    (fn [this]
      (let [{:keys [listener]} (reagent/state this)]
        (.removeEventListener js/window "resize" listener)))
    :render
    (fn [this]
      (let [{:keys [bounds]} (reagent/state this)
            {:keys [process]} (reagent/props this)
            {:keys [pi]} process]
        [:div.Network
         [:svg (map-indexed (fn [n _]
                              [node {:key n :bounds bounds :n n :total (count pi)}])
                            pi)]]))}))
