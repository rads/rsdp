(ns rads.rsdp.modules.synchronous-job-handler-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [rads.rsdp.modules.synchronous-job-handler :as job-handler]))

(deftest advance-test
  (testing "processes jobs"
    (let [state nil
          job ::test-job
          event [::jh :submit job]
          processed (atom [])
          config {:jh ::jh
                  :process #(swap! processed conj %)}]
      (job-handler/advance state event config)
      (is (= [::test-job] @processed))))
  (testing "triggers confirm events"
    (let [state nil
          job ::test-job
          event [::jh :submit job]
          config {:jh ::jh
                  :process (fn [_])}
          {:keys [trigger]} (job-handler/advance state event config)]
      (is (= [[::jh :confirm job]] trigger)))))
