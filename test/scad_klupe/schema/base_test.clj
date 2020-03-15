(ns scad-klupe.schema.base-test
  (:require [clojure.spec.alpha :as spec]
            [clojure.test :refer [deftest testing is]]
            [scad-klupe.schema.base :as schema]))

(deftest bolt-length
  "The full spec for bolt length parameters."
  (testing "Empty input."
    (is (= false (spec/valid? ::schema/bolt-length-parameters {}))))
  (testing "Head length only."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1}))))
  (testing "Total length only."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:total-length 1})))
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:total-length nil}))))
  (testing "Head length and specific total length."
    (is (= true (spec/valid? ::schema/bolt-length-parameters
                  {:head-length 1, :total-length 1}))))
  (testing "Zeroes."  ; Permitted for total length only.
    (is (= true (spec/valid? ::schema/bolt-length-parameters
                  {:head-length 0, :total-length 1})))
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1, :total-length 0}))))
  (testing "Negative numbers."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length -1, :total-length 1})))
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1, :total-length -1}))))
  (testing "Non-integer numbers."
    (is (= true (spec/valid? ::schema/bolt-length-parameters
                  {:head-length 0.1, :total-length 1/2}))))
  (testing "Head and total with a type error."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length "1", :total-length 1})))
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1, :total-length "1"}))))
  (testing "Head length and specific unthreaded length."
    (is (= true (spec/valid? ::schema/bolt-length-parameters
                  {:head-length 1, :unthreaded-length 1}))))
  (testing "Head length and unthreaded with a type error."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1, :unthreaded-length "1"}))))
  (testing "Head length and unthreaded but with nil."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1, :unthreaded-length nil}))))
  (testing "Head, total, and unthreaded."
    (is (= true (spec/valid? ::schema/bolt-length-parameters
                  {:head-length 1, :total-length 2, :unthreaded-length 1}))))
  (testing "All lengths."
    (is (= true (spec/valid? ::schema/bolt-length-parameters
                  {:head-length 1, :total-length 3, :unthreaded-length 1,
                   :threaded-length 1}))))
  (testing "All lengths but with nil."
    (is (= false (spec/valid? ::schema/bolt-length-parameters
                   {:head-length 1, :total-length 3, :unthreaded-length 1,
                    :threaded-length nil})))))

