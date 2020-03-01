(ns scad-klupe.iso-test
  (:require [clojure.test :refer [deftest testing is]]
            [scad-klupe.iso :as iso]))

(deftest length-predictor
  "Testing the predictor of total bolt length."
  (let [run (fn [& {:as parameters}]
              (iso/bolt-length
                (merge {:m-diameter 3, :head-type :hex} parameters)))]
    (testing "Inadequate parameter set."
      (is (thrown? java.lang.AssertionError (run))))
    (testing "Contradictory parameter set."  ; Total shorter than head.
      (is (thrown? java.lang.AssertionError (run :total-length 1))))
    (testing "Total length only."
      (is (= (run :total-length 2) 2))  ; Equal to head.
      (is (= (run :total-length 3) 3)))
    (testing "Internally coherent redundance."
      (is (= (run :total-length 7 :unthreaded-length 2 :threaded-length 3) 7)))
    (testing "Sum with head, without explicit total length."
      (is (= (run :unthreaded-length 2 :threaded-length 3) 7)))))
