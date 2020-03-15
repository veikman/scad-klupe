;;; Parameter schema for scad-klupe.iso.
;;; This is intended partly for parameter validation within scad-klupe and
;;; partly for use in the configuration layer of applications.

(ns scad-klupe.schema.iso
  (:require [clojure.spec.alpha :as spec]
            [scad-klupe.schema.base :as base]))

;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERNAL CONSTANTS ;;
;;;;;;;;;;;;;;;;;;;;;;;;

;; Please use scad-klupe.iso/datum in place of data in this section.
;; Contents here may change without notice.

(spec/def ::iso-property #{:hex-head-short-diagonal
                           :hex-head-long-diagonal
                           :head-hex-drive-short-diagonal
                           :head-hex-drive-long-diagonal
                           :iso4017-hex-head-length-nominal
                           :hex-nut-height
                           :socket-diameter
                           :socket-height
                           :button-diameter
                           :button-height
                           :countersunk-diameter
                           :countersunk-height
                           :thread-pitch-coarse
                           :iso7089-inner-diameter
                           :iso7089-outer-diameter
                           :iso7089-thickness})

(def ^:internal iso-data
  "Various constants from ISO metric fastener standards.
  This is a map of nominal ISO bolt diameter (in mm) to various other
  measurements according to spec. Instead of relying on this raw data in
  applications, prefer the more capable datum function."
  {3 {:socket-diameter 5.5
      :hex-nut-height 2.4
      :iso4017-hex-head-length-nominal 2
      :thread-pitch-coarse 0.5
      :head-hex-drive-short-diagonal 2.5
      :iso7089-inner-diameter 3.2
      :iso7089-outer-diameter 7
      :iso7089-thickness 0.5}
   4 {:socket-diameter 7
      :hex-nut-height 3.2
      :iso4017-hex-head-length-nominal 2.8
      :thread-pitch-coarse 0.7
      :head-hex-drive-short-diagonal 3
      :iso7089-inner-diameter 4.3
      :iso7089-outer-diameter 9
      :iso7089-thickness 0.8}
   5 {:socket-diameter 8.5
      :hex-head-short-diagonal 8
      :hex-nut-height 4.7
      :iso4017-hex-head-length-nominal 3.5
      :thread-pitch-coarse 0.8
      :head-hex-drive-short-diagonal 4
      :iso7089-inner-diameter 5.3
      :iso7089-outer-diameter 10
      :iso7089-thickness 1}
   6 {:socket-diameter 10
      :hex-nut-height 5.2
      :iso4017-hex-head-length-nominal 4
      :thread-pitch-coarse 1
      :head-hex-drive-short-diagonal 5
      :iso7089-inner-diameter 6.4
      :iso7089-outer-diameter 12
      :iso7089-thickness 1.6}
   8 {:socket-diameter 13
      :hex-nut-height 6.8
      :iso4017-hex-head-length-nominal 5.3
      :thread-pitch-coarse 1.25
      :head-hex-drive-short-diagonal 6
      :iso7089-inner-diameter 8.4
      :iso7089-outer-diameter 16
      :iso7089-thickness 1.6}})


;;;;;;;;;;;;;:;;;;;;;;;
;; INTERFACE PARSERS ;;
;;;;;;;;;;;;;;;;;;;;;;;

;; Functions for parsing serialized parameters in applications.

(def bolt-parsers {:m-diameter num
                   :pitch num
                   :angle num
                   :total-length num
                   :unthreaded-length num
                   :threaded-length num
                   :head-type keyword
                   :drive-type keyword
                   :point-type keyword
                   :resolution num
                   :include-threading boolean
                   :negative boolean})


;;;;;;;;;;;;;;;;;;;;;
;; INTERFACE SPECS ;;
;;;;;;;;;;;;;;;;;;;;;

;; The following items are exposed for use in application data validation.

;; Individual items in parameter maps:
(spec/def ::m-diameter #(contains? iso-data %))  ; Nominal fastener diameter.

;; Supported types of bolt heads, drives and points:
(spec/def ::head-type #{:hex     ; Hex head with the diameter of a nut.
                        :socket  ; Full cylindrical counterbore cap.
                        :button  ; Partial (low, smooth-edged) socket cap.
                        :countersunk}) ; Flat head tapering toward the bolt.
(spec/def ::drive-type #{:hex})
(spec/def ::point-type #{:cone})

;; Parameters to the bolt function follow.

;; First, a spec that does not require length, to allow an application
;; to first validate the user’s input and then inject a default length
;; that does not override the user’s choice of parameter.
(spec/def ::bolt-parameter-keys
  (spec/keys :req-un [::m-diameter ::head-type]
             :opt-un [::base/pitch ::base/angle ::base/total-length
                      ::base/unthreaded-length ::base/threaded-length
                      ::drive-type ::point-type
                      ::base/resolution
                      ::base/include-threading ::base/negative]))

;; Third, the more complete spec used by scad-klupe itself.
(spec/def ::bolt-parameters
  (spec/and
    ::bolt-parameter-keys
    ::base/bolt-length-specifiers))
