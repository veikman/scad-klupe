(ns scad-klupe.iso-test
  (:require [clojure.test :refer [deftest testing is]]
            [scad-klupe.iso :as iso]))


(deftest length-predictor
  "Testing the predictor of total bolt length."
  (let [run (fn [& {:as parameters}]
              "Run the function under test for head length 2."
              (iso/bolt-length
                (merge {:m-diameter 3, :head-type :hex} parameters)))]
    (testing "Inadequate parameter set."
      (is (thrown? java.lang.AssertionError (iso/bolt-length {})))
      (is (thrown? java.lang.AssertionError (run))))
    (testing "Zero total length."
      (is (thrown? java.lang.AssertionError (run :total-length 0))))
    (testing "Negative numbers."
      (is (thrown? java.lang.AssertionError (run :total-length -10)))
      (is (thrown? java.lang.AssertionError (run :unthreaded-length -10)))
      (is (thrown? java.lang.AssertionError (run :threaded-length -10))))
    (testing "Contradiction: Total shorter than head."
      (is (thrown? java.lang.AssertionError (run :total-length 1))))
    (testing "Contradiction: Total not accounting for head."
      (is (thrown? java.lang.AssertionError (run :total-length 10
                                                 :unthreaded-length 10))))
    (testing "Total length only."
      (is (= 2 (run :total-length 2)))  ; Equal to head.
      (is (= 3 (run :total-length 3)))) ; Longer than head.
    (testing "Internally coherent redundance."
      (is (= 7 (run :total-length 7 :unthreaded-length 2 :threaded-length 3))))
    (testing "Sum with head, without explicit total length."
      (is (= 7 (run :unthreaded-length 2 :threaded-length 3)))
      (is (= 4 (run :threaded-length 2))))))
