(ns rads.rsdp)

(defprotocol ILinks
  (poll! [links])
  (publish! [links steps]))

(defprotocol IModule
  (advance [module event]))

(defn trigger [module event]
  (update module :out conj event))

(defn start-timer [module delta]
  (update module :timers conj delta))
