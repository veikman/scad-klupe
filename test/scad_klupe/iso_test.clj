(ns scad-klupe.iso-test
  (:require [clojure.test :refer [deftest testing is]]
            [scad-klupe.iso :as iso]))

(deftest length-predictor
    "Testing the predictor of total bolt length."
    (testing "Inadequate parameter set."
      (is (thrown? java.lang.AssertionError
            (iso/total-bolt-length {:m-diameter 3}))))
    (testing "Total length only."
      (is (= (iso/total-bolt-length
               {:m-diameter 3, :total-length 1})
             1)))
    (testing "Head type only."
      (is (= (iso/total-bolt-length
               {:m-diameter 3, :head-type :countersunk})
             3/2)))
    (testing "Mutual redundance."
      (is (= (iso/total-bolt-length
               {:m-diameter 3, :total-length 1, :unthreaded-length 2,
                :threaded-length 3, :head-type :countersunk})
             1)))
    (testing "Sum without explicit total length."
      (is (= (iso/total-bolt-length
               {:m-diameter 3, :unthreaded-length 2,
                :threaded-length 3, :head-type :countersunk})
             13/2))))
