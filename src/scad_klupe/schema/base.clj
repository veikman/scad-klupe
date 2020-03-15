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
(spec/def ::non-negative (spec/and number? (complement neg?)))
(spec/def ::optional (spec/nilable ::non-negative))

;; Individual items within common parameter maps.
(spec/def ::pitch pos?)
(spec/def ::angle pos?)
(spec/def ::total-length pos?)
(spec/def ::head-length ::non-negative)
(spec/def ::unthreaded-length ::non-negative)
(spec/def ::threaded-length ::non-negative)
(spec/def ::resolution pos?)
(spec/def ::include-threading boolean?)
(spec/def ::negative boolean?)

;; Parameters to bolt length computing functions.
;; Basic item-local spec:
(spec/def ::bolt-length-parameter-keys
  (spec/keys :req-un [::head-length]
             :opt-un [::total-length ::unthreaded-length ::threaded-length]))
;; A larger spec extending to arithmetic relationships:
(spec/def ::bolt-length-parameters
  (spec/and
    ::bolt-length-parameter-keys
    (fn [{:keys [total-length unthreaded-length threaded-length]}]
      "Require at least one item beyond head-length."
      (or total-length unthreaded-length threaded-length))
    (fn [{:keys [total-length head-length unthreaded-length threaded-length]}]
      "When total, unthreaded and threaded bolt lengths are all specified, the
      total length must be the sum of unthreaded length, threaded length and
      the length of the head."
      (if (and total-length unthreaded-length threaded-length)
        (= total-length (+ unthreaded-length threaded-length head-length))
        true))
    (fn [{:keys [total-length unthreaded-length head-length]}]
      "When total and unthreaded lengths are both specified, total bolt length
      cannot be smaller than the sum of unthreaded and head lengths."
      (if (and total-length unthreaded-length)
        (>= total-length (+ unthreaded-length head-length))
        true))
    (fn [{:keys [total-length threaded-length head-length]}]
      "When total and threaded lengths are both specified, total bolt length
      cannot be smaller than the sum of threaded and head lengths."
      (if (and total-length threaded-length)
        (>= total-length (+ threaded-length head-length))
        true))
    (fn [{:keys [total-length head-length]}]
      "Total bolt length cannot be shorter than head length, which is to be
      based on the diameter of the bolt and its head type."
      (if total-length (>= total-length head-length) true))))

