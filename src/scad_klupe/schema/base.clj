;;; Parameter schema for scad-klupe.base.
;;; This is intended partly for parameter validation within scad-klupe and
;;; partly for use in the configuration layer of applications.

(ns scad-klupe.schema.base
  (:require [clojure.spec.alpha :as spec]))


;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE SPECS ;;
;;;;;;;;;;;;;;;;;;;;;

;; The following items are exposed for use in application data validation.

;; Primitives.
(spec/def ::non-negative (spec/and #(number? %) #(not (neg? %))))
(spec/def ::optional (spec/nilable ::non-negative))

;; Individual items within parameter maps.
(spec/def :length/head ::non-negative)
(spec/def :length/total ::optional)
(spec/def :length/unthreaded ::optional)
(spec/def :length/threaded ::optional)

;; Parameters to bolt length computing functions.
;; Basic item-local spec:
(spec/def ::bolt-length-parameter-keys
  (spec/keys :req-un [:length/head]
             :opt-un [:length/total :length/unthreaded :length/threaded]))
;; A larger spec extending to arithmetic relationships:
(spec/def ::bolt-length-parameters
  (spec/and
    ::bolt-length-parameter-keys
    (fn [{:keys [total unthreaded threaded head]}]
      "When total, unthreaded and threaded bolt lengths are all specified, the
      total length must be the sum of unthreaded length, threaded length and
      the length of the head."
      (if (and total unthreaded threaded)
        (= total (+ unthreaded threaded head))
        true))
    (fn [{:keys [total unthreaded head]}]
      "When total and unthreaded lengths are both specified, total bolt length
      cannot be smaller than the sum of unthreaded and head lengths."
      (if (and total unthreaded)
        (>= total (+ unthreaded head))
        true))
    (fn [{:keys [total threaded head]}]
      "When total and threaded lengths are both specified, total bolt length
      cannot be smaller than the sum of threaded and head lengths."
      (if (and total threaded)
        (>= total (+ threaded head))
        true))
    (fn [{:keys [total head]}]
      "Total bolt length cannot be shorter than head length, which is to be
      based on the diameter of the bolt and its head type."
      (if total (>= total head) true))))

