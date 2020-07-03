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
(spec/def ::positive (spec/and number? pos?))
(spec/def ::non-negative (spec/and number? (complement neg?)))

;; Individual items within common parameter maps.
(spec/def ::pitch ::positive)
(spec/def ::angle ::positive)
(spec/def ::total-length ::positive)
(spec/def ::head-length ::non-negative)
(spec/def ::unthreaded-length ::non-negative)
(spec/def ::threaded-length ::non-negative)
(spec/def ::channel-diameter ::non-negative)
(spec/def ::channel-length ::positive)
(spec/def ::resolution ::positive)
(spec/def ::include-threading boolean?)
(spec/def ::negative boolean?)

;; Basic item-local spec for computing bolt length:
(spec/def ::bolt-length-parameter-keys
  (spec/keys :req-un [::head-length]
             :opt-un [::total-length ::unthreaded-length ::threaded-length]))

;; A check for the mere presence of at least one length specifier other than
;; head length. This could have used spec’s or, but naming the options is not
;; believed to be worth the added complexity of conform/unform.
(spec/def ::bolt-length-specifiers
  (fn [{:keys [total-length unthreaded-length threaded-length]}]
    (some? (or total-length unthreaded-length threaded-length))))

;; Arithmetic relationships between optional values for bolt length:
(spec/def ::bolt-length-relationships
  (spec/and
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

;; A composite spec:
(spec/def ::bolt-length-parameters
  (spec/and ::bolt-length-parameter-keys
            ::bolt-length-specifiers
            ::bolt-length-relationships))

