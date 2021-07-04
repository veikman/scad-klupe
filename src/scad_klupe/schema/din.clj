;;; Data for scad-klupe.din. Space reserved for a schema to match.

(ns scad-klupe.schema.din
  (:require [clojure.spec.alpha :as spec]))

;;;;;;;;;;;;;;;;;;;;;;;;
;; INTERNAL CONSTANTS ;;
;;;;;;;;;;;;;;;;;;;;;;;;

;; Please use scad-klupe.din/datum as an interface, not data in this section.
;; Contents here may change without notice.

(spec/def ::din-property #{:din562-side
                           :din562-height})  ; Default and minimum height.

(def ^:internal din-data
  "DIN data in relation to ISO standards.
  This is a map of nominal ISO bolt diameter (in mm) to various DIN-standard
  measurements. Instead of relying on this raw data in applications, prefer the
  more capable datum function."
  {3 {:din562-side 5.5
      :din562-height 1.6}
   4 {:din562-side 7
      :din562-height 1.8}
   5 {:din562-side 8
      :din562-height 2.3}
   6 {:din562-side 10
      :din562-height 2.72}
   8 {:din562-side 13
      :din562-height 3.52}
   10 {:din562-side 17
       :din562-height 4.52}})


